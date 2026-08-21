package com.jason.yang.asset.application.service;

import com.jason.yang.asset.application.batch.BatchCommand;
import com.jason.yang.asset.application.batch.BatchResult;
import com.jason.yang.asset.application.batch.ProcessOrderBatchUseCase;
import com.jason.yang.asset.application.batch.RunTriageQueueResult;
import com.jason.yang.asset.application.port.BatchProcessorProvider;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class DefaultRunTriageQueueServiceTest {

    @Test
    void mapsSharedExecutionCommandToExistingBatchUseCase() {
        CapturingProcessor processor = new CapturingProcessor();
        CapturingProvider provider = new CapturingProvider(processor);
        DefaultRunTriageQueueService service = new DefaultRunTriageQueueService(provider);
        RunTriageQueueResult execution = service.run();
        BatchResult result = execution.batchResult();

        assertSame(processor.result, result);
        assertEquals(path("materials"), provider.materialsDirectory);
        assertEquals(path("build/audit.jsonl"), provider.auditFile);
        assertEquals(java.time.Instant.parse("2026-07-28T12:00:00Z"), provider.evaluationTime);
        assertEquals(path("materials/orders.jsonl"), processor.command.ordersFile());
        assertEquals(path("build/decisions.jsonl"), processor.command.outputFile());
        assertEquals(processor.command.runId(), result.runId());
        assertEquals(1, processor.command.maxConcurrency());
        assertEquals(path("build/decisions.jsonl"), execution.outputFile());
        assertEquals(path("build/audit.jsonl"), execution.auditFile());
    }

    private Path path(String value) {
        return Paths.get(value).toAbsolutePath().normalize();
    }

    private static final class CapturingProvider implements BatchProcessorProvider {
        private final ProcessOrderBatchUseCase processor;
        private Path materialsDirectory;
        private Path auditFile;
        private java.time.Instant evaluationTime;

        private CapturingProvider(ProcessOrderBatchUseCase processor) {
            this.processor = processor;
        }

        @Override
        public ProcessOrderBatchUseCase get(Path materialsDirectory, Path auditFile,
                                             java.time.Instant evaluationTime) {
            this.materialsDirectory = materialsDirectory;
            this.auditFile = auditFile;
            this.evaluationTime = evaluationTime;
            return processor;
        }
    }

    private static final class CapturingProcessor implements ProcessOrderBatchUseCase {
        private BatchResult result;
        private BatchCommand command;

        @Override
        public BatchResult process(BatchCommand command) {
            this.command = command;
            this.result = new BatchResult(
                    command.runId(), 14, 0,
                    Collections.singletonMap("AUTO_COMPLETE", 2L), Duration.ofMillis(10));
            return result;
        }
    }
}
