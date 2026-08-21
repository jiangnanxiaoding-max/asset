package com.jason.yang.asset.adapter.web;

import com.jason.yang.asset.application.batch.BatchCommand;
import com.jason.yang.asset.application.batch.BatchResult;
import com.jason.yang.asset.application.evaluation.EvaluateTriageUseCase;
import com.jason.yang.asset.application.evaluation.EvaluationCommand;
import com.jason.yang.asset.application.evaluation.EvaluationFailure;
import com.jason.yang.asset.application.evaluation.EvaluationReport;
import com.jason.yang.asset.infrastructure.config.OfflineTriageRuntimeFactory;
import com.jason.yang.asset.infrastructure.config.OfflineTriageRuntime;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;

/** HTTP test adapter exposing the same file-driven triage and evaluation flows as the CLI. */
@RestController
@Profile("!cli")
@RequestMapping("/api/v1/test")
public final class TriageController {
    static final String REQUEST_ID_ATTRIBUTE = TriageController.class.getName() + ".requestId";
    private static final Logger log = LoggerFactory.getLogger(TriageController.class);

    private final OfflineTriageRuntimeFactory runtimeFactory;
    private final EvaluateTriageUseCase evaluationUseCase;
    private final Path materials;
    private final Path orders;
    private final Path output;
    private final Path audit;
    private final Path goldenCases;
    private final Path evaluationReport;
    private final Instant evaluationTime;
    private final int maxConcurrency;
    private final ReentrantLock executionLock = new ReentrantLock();

    public TriageController(
            OfflineTriageRuntimeFactory runtimeFactory,
            EvaluateTriageUseCase evaluationUseCase,
            @Value("${asset.materials:materials}") String materials,
            @Value("${asset.orders-file:materials/orders.jsonl}") String orders,
            @Value("${asset.output-file:build/decisions.jsonl}") String output,
            @Value("${asset.audit-file:build/audit.jsonl}") String audit,
            @Value("${asset.golden-cases:evaluation/golden-cases.json}") String goldenCases,
            @Value("${asset.evaluation-report:build/evaluation-report.json}") String evaluationReport,
            @Value("${asset.evaluation-time:2026-07-28T12:00:00Z}") Instant evaluationTime,
            @Value("${asset.max-concurrency:1}") int maxConcurrency
    ) {
        this.runtimeFactory = runtimeFactory;
        this.evaluationUseCase = evaluationUseCase;
        this.materials = normalized(materials);
        this.orders = normalized(orders);
        this.output = normalized(output);
        this.audit = normalized(audit);
        this.goldenCases = normalized(goldenCases);
        this.evaluationReport = normalized(evaluationReport);
        this.evaluationTime = evaluationTime;
        this.maxConcurrency = maxConcurrency;
    }

    /** Reads the configured JSONL queue and writes the same decision and audit files as CLI triage. */
    @PostMapping(value = "/triage", produces = MediaType.APPLICATION_JSON_VALUE)
    public BatchTestResponse triage(
            @RequestHeader(value = "X-Request-Id", required = false) String suppliedRequestId,
            HttpServletRequest request
    ) {
        String requestId = prepareRequest(request, suppliedRequestId);
        String runId = "web-" + requestId;
        log.info("web batch triage started requestId={} runId={} orders={}",
                requestId, runId, orders.getFileName());
        executionLock.lock();
        try {
            /**
             * 初始化执行上下文
             */
            OfflineTriageRuntime runtime = runtimeFactory.create(materials, audit, evaluationTime);
            
            BatchResult result = runtime.batchUseCase().process(new BatchCommand(
                    orders, output, runId, evaluationTime, maxConcurrency));
            log.info("web batch triage completed requestId={} total={} failed={}",
                    requestId, result.total(), result.failed());
            return BatchTestResponse.from(requestId, result, output, audit);
        } finally {
            executionLock.unlock();
        }
    }

    /** Runs the configured golden cases through the same evaluation use case as CLI evaluate. */
    @PostMapping(value = "/evaluate", produces = MediaType.APPLICATION_JSON_VALUE)
    public EvaluationTestResponse evaluate(
            @RequestHeader(value = "X-Request-Id", required = false) String suppliedRequestId,
            HttpServletRequest request
    ) {
        String requestId = prepareRequest(request, suppliedRequestId);
        log.info("web evaluation started requestId={} goldenCases={}",
                requestId, goldenCases.getFileName());
        executionLock.lock();
        try {
            EvaluationReport result = evaluationUseCase.evaluate(
                    new EvaluationCommand(materials, goldenCases, evaluationReport));
            log.info("web evaluation completed requestId={} cases={} failed={} unsafe={}",
                    requestId, result.cases(), result.failed(), result.unsafeAutoCompletions());
            return EvaluationTestResponse.from(requestId, result, evaluationReport);
        } finally {
            executionLock.unlock();
        }
    }

    private String prepareRequest(HttpServletRequest request, String suppliedRequestId) {
        String requestId = validRequestId(suppliedRequestId)
                ? suppliedRequestId : UUID.randomUUID().toString();
        request.setAttribute(REQUEST_ID_ATTRIBUTE, requestId);
        return requestId;
    }

    private boolean validRequestId(String value) {
        return value != null && !value.trim().isEmpty() && value.length() <= 100
                && value.matches("[A-Za-z0-9._-]+");
    }

    private static Path normalized(String path) {
        return java.nio.file.Paths.get(path).toAbsolutePath().normalize();
    }

    public static final class BatchTestResponse {
    private final String requestId;
    private final String runId;
    private final long total;
    private final long failed;
    private final Map<String, Long> dispositionCounts;
    private final long elapsedMillis;
    private final String outputFile;
    private final String auditFile;

    public BatchTestResponse(String requestId, String runId, long total, long failed, Map<String, Long> dispositionCounts, long elapsedMillis, String outputFile, String auditFile) {

        this.requestId = requestId;
        this.runId = runId;
        this.total = total;
        this.failed = failed;
        this.dispositionCounts = dispositionCounts;
        this.elapsedMillis = elapsedMillis;
        this.outputFile = outputFile;
        this.auditFile = auditFile;
    }

    public String requestId() {
        return requestId;
    }

    public String getRequestId() {
        return requestId;
    }

    public String runId() {
        return runId;
    }

    public String getRunId() {
        return runId;
    }

    public long total() {
        return total;
    }

    public long getTotal() {
        return total;
    }

    public long failed() {
        return failed;
    }

    public long getFailed() {
        return failed;
    }

    public Map<String, Long> dispositionCounts() {
        return dispositionCounts;
    }

    public Map<String, Long> getDispositionCounts() {
        return dispositionCounts;
    }

    public long elapsedMillis() {
        return elapsedMillis;
    }

    public long getElapsedMillis() {
        return elapsedMillis;
    }

    public String outputFile() {
        return outputFile;
    }

    public String getOutputFile() {
        return outputFile;
    }

    public String auditFile() {
        return auditFile;
    }

    public String getAuditFile() {
        return auditFile;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof BatchTestResponse)) return false;
        BatchTestResponse that = (BatchTestResponse) other;
        return java.util.Objects.equals(requestId, that.requestId)
                && java.util.Objects.equals(runId, that.runId)
                && total == that.total
                && failed == that.failed
                && java.util.Objects.equals(dispositionCounts, that.dispositionCounts)
                && elapsedMillis == that.elapsedMillis
                && java.util.Objects.equals(outputFile, that.outputFile)
                && java.util.Objects.equals(auditFile, that.auditFile);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(requestId, runId, total, failed, dispositionCounts, elapsedMillis, outputFile, auditFile);
    }

    @Override
    public String toString() {
        return "BatchTestResponse{" + "requestId=" + requestId + ", runId=" + runId + ", total=" + total + ", failed=" + failed + ", dispositionCounts=" + dispositionCounts + ", elapsedMillis=" + elapsedMillis + ", outputFile=" + outputFile + ", auditFile=" + auditFile + "}";
    }


        static BatchTestResponse from(
                String requestId, BatchResult result, Path outputFile, Path auditFile
        ) {
            return new BatchTestResponse(
                    requestId, result.runId(), result.total(), result.failed(),
                    result.dispositionCounts(), result.elapsed().toMillis(),
                    outputFile.toString(), auditFile.toString());
        }
    }

    public static final class EvaluationTestResponse {
    private final String requestId;
    private final int cases;
    private final int passed;
    private final int failed;
    private final int unsafeAutoCompletions;
    private final boolean successful;
    private final List<EvaluationFailure> failures;
    private final String reportFile;

    public EvaluationTestResponse(String requestId, int cases, int passed, int failed, int unsafeAutoCompletions, boolean successful, List<EvaluationFailure> failures, String reportFile) {

        this.requestId = requestId;
        this.cases = cases;
        this.passed = passed;
        this.failed = failed;
        this.unsafeAutoCompletions = unsafeAutoCompletions;
        this.successful = successful;
        this.failures = failures;
        this.reportFile = reportFile;
    }

    public String requestId() {
        return requestId;
    }

    public String getRequestId() {
        return requestId;
    }

    public int cases() {
        return cases;
    }

    public int getCases() {
        return cases;
    }

    public int passed() {
        return passed;
    }

    public int getPassed() {
        return passed;
    }

    public int failed() {
        return failed;
    }

    public int getFailed() {
        return failed;
    }

    public int unsafeAutoCompletions() {
        return unsafeAutoCompletions;
    }

    public int getUnsafeAutoCompletions() {
        return unsafeAutoCompletions;
    }

    public boolean successful() {
        return successful;
    }

    public boolean getSuccessful() {
        return successful;
    }

    public List<EvaluationFailure> failures() {
        return failures;
    }

    public List<EvaluationFailure> getFailures() {
        return failures;
    }

    public String reportFile() {
        return reportFile;
    }

    public String getReportFile() {
        return reportFile;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof EvaluationTestResponse)) return false;
        EvaluationTestResponse that = (EvaluationTestResponse) other;
        return java.util.Objects.equals(requestId, that.requestId)
                && cases == that.cases
                && passed == that.passed
                && failed == that.failed
                && unsafeAutoCompletions == that.unsafeAutoCompletions
                && successful == that.successful
                && java.util.Objects.equals(failures, that.failures)
                && java.util.Objects.equals(reportFile, that.reportFile);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(requestId, cases, passed, failed, unsafeAutoCompletions, successful, failures, reportFile);
    }

    @Override
    public String toString() {
        return "EvaluationTestResponse{" + "requestId=" + requestId + ", cases=" + cases + ", passed=" + passed + ", failed=" + failed + ", unsafeAutoCompletions=" + unsafeAutoCompletions + ", successful=" + successful + ", failures=" + failures + ", reportFile=" + reportFile + "}";
    }


        static EvaluationTestResponse from(
                String requestId, EvaluationReport result, Path reportFile
        ) {
            return new EvaluationTestResponse(
                    requestId, result.cases(), result.passed(), result.failed(),
                    result.unsafeAutoCompletions(), result.successful(), result.failures(),
                    reportFile.toString());
        }
    }
}
