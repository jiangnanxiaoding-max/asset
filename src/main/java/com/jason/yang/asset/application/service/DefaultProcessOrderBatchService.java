package com.jason.yang.asset.application.service;

import com.jason.yang.asset.application.TriageOrderUseCase;
import com.jason.yang.asset.application.TriageCommand;
import com.jason.yang.asset.application.batch.BatchCommand;
import com.jason.yang.asset.application.batch.BatchResult;
import com.jason.yang.asset.application.batch.ProcessOrderBatchUseCase;
import com.jason.yang.asset.application.batch.TriageOutputRecord;
import com.jason.yang.asset.application.input.InputViolation;
import com.jason.yang.asset.application.input.OrderParseResult;
import com.jason.yang.asset.application.input.OrderParser;
import com.jason.yang.asset.application.input.RawOrderEnvelope;
import com.jason.yang.asset.application.model.TriageResult;
import com.jason.yang.asset.application.port.BatchAuditPort;
import com.jason.yang.asset.application.port.DecisionOutputPort;
import com.jason.yang.asset.application.port.OrderProcessingPort;
import com.jason.yang.asset.domain.DecisionId;
import com.jason.yang.asset.domain.Disposition;
import com.jason.yang.asset.domain.OrderId;
import com.jason.yang.asset.domain.Order;
import com.jason.yang.asset.domain.ReasonCode;
import com.jason.yang.asset.domain.TriageDecision;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** Application service implementing deterministic, fail-closed JSONL batch processing. */
public final class DefaultProcessOrderBatchService implements ProcessOrderBatchUseCase {
    private static final Logger log = LoggerFactory.getLogger(DefaultProcessOrderBatchService.class);
    private final OrderParser parser;
    private final TriageOrderUseCase triageUseCase;
    private final OrderProcessingPort processingPort;
    private final DecisionOutputPort outputPort;
    private final BatchAuditPort batchAuditPort;

    public DefaultProcessOrderBatchService(
            OrderParser parser,
            TriageOrderUseCase triageUseCase,
            OrderProcessingPort processingPort,
            DecisionOutputPort outputPort,
            BatchAuditPort batchAuditPort
    ) {
        this.parser = Objects.requireNonNull(parser);
        this.triageUseCase = Objects.requireNonNull(triageUseCase);
        this.processingPort = Objects.requireNonNull(processingPort);
        this.outputPort = Objects.requireNonNull(outputPort);
        this.batchAuditPort = Objects.requireNonNull(batchAuditPort);
    }

    @Override
    public BatchResult process(BatchCommand command) {
        Instant started = Instant.now();
        log.info("batch started runId={} ordersFile={} maxConcurrency={}",
                command.runId(), command.ordersFile().getFileName(), command.maxConcurrency());
        outputPort.initialize(command.outputFile());
        EnumMap<Disposition, Long> counts = new EnumMap<>(Disposition.class);
        long total = 0;
        long failed = 0;

        try (BufferedReader reader = Files.newBufferedReader(command.ordersFile(), StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                total++;
                if (line.trim().isEmpty()) {
                    continue;
                }
                try {
                    Disposition disposition = processLine(command, total, line);
                    counts.merge(disposition, 1L, Long::sum);
                } catch (RuntimeException exception) {
                    failed++;
                    log.error("batch line failed runId={} sourcePosition={}",
                            command.runId(), total, exception);
                    try {
                        writeInternalFailure(command, total, line);
                    } catch (RuntimeException auditException) {
                        log.error("failed line could not be audited runId={} sourcePosition={}",
                                command.runId(), total, auditException);
                        writeUnauditedFailure(command, total);
                    }
                    counts.merge(Disposition.MANUAL_REVIEW, 1L, Long::sum);
                }
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot read orders file: " + command.ordersFile(), exception);
        }

        Duration elapsed = Duration.between(started, Instant.now());
        Map<String, Long> namedCounts = new java.util.LinkedHashMap<>();
        counts.forEach((key, value) -> namedCounts.put(key.name(), value));
        log.info("batch completed runId={} total={} failed={} elapsedMs={}",
                command.runId(), total, failed, elapsed.toMillis());
        return new BatchResult(command.runId(), total, failed, namedCounts, elapsed);
    }

    /**
     * 解析消费order消息数据
     */
    private Disposition processLine(BatchCommand command, long position, String line) {
        String hash = sha256(line);
        /**
         * 封存源数据
         */
        RawOrderEnvelope envelope = new RawOrderEnvelope(
                command.ordersFile().getFileName().toString(), position, line, hash, command.evaluationTime());
        OrderParseResult parseResult = parser.parse(envelope);
        if (parseResult instanceof OrderParseResult.Invalid) {
            OrderParseResult.Invalid invalid = (OrderParseResult.Invalid) parseResult;
            String orderId = invalid.orderId().orElse("INVALID-LINE-" + position);
            return writeRejected(command, position, hash, orderId, Disposition.INVALID_INPUT,
                    Collections.singletonList(ReasonCode.MALFORMED_ORDER), invalid.violations());
        }

        Order order = ((OrderParseResult.Parsed) parseResult).order();
        /**
         * order执行状态幂等判断
         */
        OrderProcessingPort.ProcessingClaim claim = processingPort.claim(order.orderId(), hash, command.runId());
        switch (claim.status()) {
            case ACQUIRED:
                return processAcquired(command, position, hash, order);
            case ALREADY_COMPLETED_SAME_PAYLOAD:
                TriageResult prior = claim.priorResult().orElseThrow(
                        () -> new IllegalStateException("Completed claim has no prior result"));
                outputPort.append(command.outputFile(), output(command, position, prior, true));
                return prior.decision().disposition();
            case ALREADY_RUNNING:
                return writeRejected(command, position, hash, order.orderId(), Disposition.HOLD,
                        Collections.singletonList(ReasonCode.ORDER_ALREADY_RUNNING),
                        Collections.<InputViolation>emptyList());
            case PAYLOAD_CONFLICT:
                return writeRejected(command, position, hash, order.orderId(), Disposition.MANUAL_REVIEW,
                        Collections.singletonList(ReasonCode.ORDER_PAYLOAD_CONFLICT),
                        Collections.<InputViolation>emptyList());
            default:
                throw new IllegalStateException("Unsupported claim status: " + claim.status());
        }
    }

    private Disposition processAcquired(
            BatchCommand command, long position, String hash, com.jason.yang.asset.domain.Order order
    ) {
        try {
            TriageResult result = triageUseCase.triage(
                    new TriageCommand(order, command.runId(), hash, position));
            processingPort.complete(order.orderId(), hash, result);
            outputPort.append(command.outputFile(), output(command, position, result, false));
            return result.decision().disposition();
        } catch (RuntimeException exception) {
            processingPort.fail(order.orderId(), hash);
            throw exception;
        }
    }

    private Disposition writeRejected(
            BatchCommand command,
            long position,
            String hash,
            String orderId,
            Disposition disposition,
            List<ReasonCode> reasonCodes,
            List<InputViolation> violations
    ) {
        String auditId = batchAuditPort.appendRejectedLine(
                command.runId(), position, hash, orderId, disposition, reasonCodes, violations);
        TriageOutputRecord record = new TriageOutputRecord(
                "1.0", command.runId(), position, "D-" + UUID.randomUUID(), orderId,
                disposition, reasonCodes, false,
                "Order was not eligible for the normal decision pipeline: " + reasonCodes,
                "policy-2026-07-28", command.evaluationTime(), auditId, false, violations
        );
        outputPort.append(command.outputFile(), record);
        return disposition;
    }

    private void writeInternalFailure(BatchCommand command, long position, String line) {
        writeRejected(command, position, sha256(line), "FAILED-LINE-" + position,
                Disposition.MANUAL_REVIEW, Collections.singletonList(ReasonCode.INTERNAL_PROCESSING_ERROR),
                Collections.<InputViolation>emptyList());
    }

    private void writeUnauditedFailure(BatchCommand command, long position) {
        outputPort.append(command.outputFile(), new TriageOutputRecord(
                "1.0", command.runId(), position, "D-" + UUID.randomUUID(),
                "FAILED-LINE-" + position, Disposition.MANUAL_REVIEW,
                java.util.Arrays.asList(ReasonCode.INTERNAL_PROCESSING_ERROR, ReasonCode.AUDIT_PERSISTENCE_FAILED),
                false, "Processing failed and the audit store was unavailable; no side effect was executed.",
                "policy-2026-07-28", command.evaluationTime(), "", false,
                Collections.<InputViolation>emptyList()
        ));
    }

    private TriageOutputRecord output(
            BatchCommand command, long position, TriageResult result, boolean replayed
    ) {
        TriageDecision decision = result.decision();
        return new TriageOutputRecord(
                "1.0", command.runId(), position, decision.decisionId().value(),
                decision.orderId().value(), decision.disposition(), decision.reasonCodes(),
                decision.fundsMovementAllowed(), result.explanation(), decision.policyVersion(),
                decision.evaluatedAt(), result.auditId(), replayed,
                Collections.<InputViolation>emptyList()
        );
    }

    private String sha256(String input) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte value : digest) {
                hex.append(String.format("%02x", value & 0xff));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
