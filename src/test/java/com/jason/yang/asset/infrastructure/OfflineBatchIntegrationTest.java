package com.jason.yang.asset.infrastructure;

import com.jason.yang.asset.application.batch.BatchCommand;
import com.jason.yang.asset.application.batch.BatchResult;
import com.jason.yang.asset.application.evaluation.EvaluationCommand;
import com.jason.yang.asset.application.evaluation.EvaluationReport;
import com.jason.yang.asset.infrastructure.config.OfflineTriageRuntime;
import com.jason.yang.asset.infrastructure.config.OfflineTriageRuntimeFactory;
import com.jason.yang.asset.infrastructure.evaluation.JsonGoldenEvaluationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OfflineBatchIntegrationTest {
    private static final Instant NOW = Instant.parse("2026-07-28T12:00:00Z");

    @TempDir
    Path tempDirectory;

    @Test
    void suppliedQueueProducesFourteenAuditedDecisions() throws IOException {
        Path materials = Paths.get("materials").toAbsolutePath();
        Path output = tempDirectory.resolve("decisions.jsonl");
        Path audit = tempDirectory.resolve("audit.jsonl");
        OfflineTriageRuntime runtime = new OfflineTriageRuntimeFactory().create(materials, audit, NOW);

        BatchResult result = runtime.batchUseCase().process(new BatchCommand(
                materials.resolve("orders.jsonl"), output, "test-run", NOW, 1));

        assertEquals(14, result.total());
        assertEquals(0, result.failed());
        assertEquals(2, result.dispositionCounts().get("AUTO_COMPLETE"));
        assertEquals(14, Files.readAllLines(output, StandardCharsets.UTF_8).size());
        assertEquals(14, Files.readAllLines(audit, StandardCharsets.UTF_8).size());
        String auditContent = new String(Files.readAllBytes(audit), StandardCharsets.UTF_8);
        assertFalse(auditContent.contains("9931"), "customer note must not leak into audit");
        assertTrue(auditContent.contains("\"agent\""), "unresolved order must contain Agent audit trace");
        assertTrue(auditContent.contains("\"provider\":\"stub\""), "default runtime must stay offline");
    }

    @Test
    void goldenEvaluationHasNoUnsafeAutoCompletion() {
        Path report = tempDirectory.resolve("report.json");
        JsonGoldenEvaluationService service = new JsonGoldenEvaluationService(new OfflineTriageRuntimeFactory());

        EvaluationReport result = service.evaluate(new EvaluationCommand(
                Paths.get("materials").toAbsolutePath(),
                Paths.get("evaluation/golden-cases.json").toAbsolutePath(),
                report));

        assertTrue(result.successful());
        assertEquals(14, result.passed());
        assertEquals(0, result.unsafeAutoCompletions());
        assertTrue(Files.isRegularFile(report));
    }
}
