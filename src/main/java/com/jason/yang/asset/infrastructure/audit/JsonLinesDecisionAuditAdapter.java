package com.jason.yang.asset.infrastructure.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jason.yang.asset.application.port.DecisionAuditPort;
import com.jason.yang.asset.application.port.BatchAuditPort;
import com.jason.yang.asset.application.input.InputViolation;
import com.jason.yang.asset.domain.Disposition;
import com.jason.yang.asset.domain.ReasonCode;
import com.jason.yang.asset.domain.InvestigationFacts;
import com.jason.yang.asset.domain.LookupResult;
import com.jason.yang.asset.domain.RuleResult;
import com.jason.yang.asset.domain.TriageDecision;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/** Append-only JSONL audit adapter. Raw payloads and customer notes are deliberately excluded. */
public final class JsonLinesDecisionAuditAdapter implements DecisionAuditPort, BatchAuditPort {
    private static final Logger log = LoggerFactory.getLogger(JsonLinesDecisionAuditAdapter.class);
    private final Path auditFile;
    private final ObjectMapper mapper;
    private final ReentrantLock appendLock = new ReentrantLock();

    public JsonLinesDecisionAuditAdapter(Path auditFile, ObjectMapper mapper) {
        this.auditFile = auditFile.toAbsolutePath().normalize();
        this.mapper = mapper;
    }

    @Override
    public String append(
            TriageDecision decision,
            InvestigationFacts facts,
            List<RuleResult> ruleResults,
            String explanation
    ) {
        return append(new AuditContext("direct", "", 0),
                decision, facts, ruleResults, explanation);
    }

    @Override
    public String append(
            AuditContext context,
            TriageDecision decision,
            InvestigationFacts facts,
            List<RuleResult> ruleResults,
            String explanation
    ) {
        String auditId = "A-" + UUID.randomUUID();
        Map<String, Object> record = new LinkedHashMap<>();
        record.put("audit_schema_version", "1.0");
        record.put("audit_id", auditId);
        record.put("recorded_at", Instant.now().toString());
        record.put("run_id", context.runId());
        record.put("source_position", context.sourcePosition());
        record.put("payload_sha256", context.payloadSha256());
        record.put("order_id", decision.orderId().value());
        record.put("customer_id", facts.order().customerIdentity().value());
        record.put("asset", facts.order().asset());
        record.put("network", facts.order().network());
        record.put("policy_version", decision.policyVersion());
        record.put("facts", factSummary(facts));
        record.put("rule_results", ruleResults.stream().map(this::ruleSummary)
                .collect(Collectors.toList()));
        record.put("decision", decision);
        record.put("explanation", explanation);

        appendLine(record);
        log.info("decision audit appended orderId={} auditId={} ruleCount={}",
                decision.orderId().value(), auditId, ruleResults.size());
        return auditId;
    }

    @Override
    public String appendRejectedLine(
            String runId,
            long sourcePosition,
            String payloadSha256,
            String orderId,
            Disposition disposition,
            List<ReasonCode> reasonCodes,
            List<InputViolation> violations
    ) {
        String auditId = "A-" + UUID.randomUUID();
        Map<String, Object> record = new LinkedHashMap<>();
        record.put("audit_schema_version", "1.0");
        record.put("audit_id", auditId);
        record.put("recorded_at", Instant.now().toString());
        record.put("run_id", runId);
        record.put("source_position", sourcePosition);
        record.put("payload_sha256", payloadSha256);
        record.put("order_id", orderId);
        record.put("disposition", disposition);
        record.put("reason_codes", reasonCodes);
        record.put("violations", violations);
        appendLine(record);
        return auditId;
    }

    private Map<String, Object> factSummary(InvestigationFacts facts) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("customer", resultType(facts.customer()));
        result.put("asset_policy", resultType(facts.assetPolicy()));
        result.put("address_risk", resultType(facts.addressRisk()));
        result.put("funding", resultType(facts.funding()));
        result.put("reference_rate", resultType(facts.referenceRate()));
        result.put("travel_rule", resultType(facts.travelRule()));
        result.put("duplicate", resultType(facts.duplicate()));
        return result;
    }

    private Map<String, Object> ruleSummary(RuleResult result) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("rule_id", result.ruleId());
        summary.put("passed", result.passed());
        summary.put("proposed_disposition", result.proposedDisposition().map(Enum::name).orElse(null));
        summary.put("reason_code", result.reasonCode().map(Enum::name).orElse(null));
        summary.put("detail", result.detail());
        return summary;
    }

    private String resultType(LookupResult<?> result) {
        if (result instanceof LookupResult.Found) return "FOUND";
        if (result instanceof LookupResult.NotFound) return "NOT_FOUND";
        if (result instanceof LookupResult.Unavailable) return "UNAVAILABLE";
        if (result instanceof LookupResult.Conflict) return "CONFLICT";
        return "NOT_APPLICABLE";
    }

    private void appendLine(Map<String, Object> record) {
        appendLock.lock();
        try {
            Path parent = auditFile.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            String json = mapper.writeValueAsString(record) + System.lineSeparator();
            Files.write(auditFile, json.getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Cannot serialize audit record", exception);
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot append audit record to " + auditFile, exception);
        } finally {
            appendLock.unlock();
        }
    }
}
