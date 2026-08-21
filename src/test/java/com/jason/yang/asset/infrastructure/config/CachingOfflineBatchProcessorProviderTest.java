package com.jason.yang.asset.infrastructure.config;

import com.jason.yang.asset.application.batch.ProcessOrderBatchUseCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

class CachingOfflineBatchProcessorProviderTest {
    @TempDir
    Path tempDirectory;

    @Test
    void reusesProcessorForSameRuntimeConfiguration() {
        CachingOfflineBatchProcessorProvider provider =
                new CachingOfflineBatchProcessorProvider(new OfflineTriageRuntimeFactory());
        Path materials = Paths.get("materials").toAbsolutePath().normalize();
        Path audit = tempDirectory.resolve("audit.jsonl");
        Instant evaluationTime = Instant.parse("2026-07-28T12:00:00Z");

        ProcessOrderBatchUseCase first = provider.get(materials, audit, evaluationTime);
        ProcessOrderBatchUseCase second = provider.get(materials.resolve("."), audit, evaluationTime);
        ProcessOrderBatchUseCase differentClock = provider.get(
                materials, audit, evaluationTime.plusSeconds(1));

        assertSame(first, second, "same Web configuration must retain in-memory idempotency state");
        assertNotSame(first, differentClock, "policy evaluation time is part of runtime identity");
    }
}
