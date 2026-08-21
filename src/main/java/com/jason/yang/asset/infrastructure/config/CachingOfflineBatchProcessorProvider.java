package com.jason.yang.asset.infrastructure.config;

import com.jason.yang.asset.application.batch.ProcessOrderBatchUseCase;
import com.jason.yang.asset.application.port.BatchProcessorProvider;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/** Reuses offline runtimes so in-memory idempotency, case and circuit state survive adapter calls. */
public class CachingOfflineBatchProcessorProvider implements BatchProcessorProvider {
    private final OfflineTriageRuntimeFactory runtimeFactory;
    private final ConcurrentMap<RuntimeKey, ProcessOrderBatchUseCase> processors =
            new ConcurrentHashMap<RuntimeKey, ProcessOrderBatchUseCase>();

    public CachingOfflineBatchProcessorProvider(OfflineTriageRuntimeFactory runtimeFactory) {
        this.runtimeFactory = Objects.requireNonNull(runtimeFactory);
    }

    @Override
    public ProcessOrderBatchUseCase get(Path materialsDirectory, Path auditFile, Instant evaluationTime) {
        final RuntimeKey key = new RuntimeKey(materialsDirectory, auditFile, evaluationTime);
        return processors.computeIfAbsent(key, ignored -> runtimeFactory.create(
                key.materialsDirectory, key.auditFile, key.evaluationTime).batchUseCase());
    }

    private static final class RuntimeKey {
        private final Path materialsDirectory;
        private final Path auditFile;
        private final Instant evaluationTime;

        private RuntimeKey(Path materialsDirectory, Path auditFile, Instant evaluationTime) {
            this.materialsDirectory = normalized(materialsDirectory, "materialsDirectory");
            this.auditFile = normalized(auditFile, "auditFile");
            this.evaluationTime = Objects.requireNonNull(evaluationTime, "evaluationTime");
        }

        private static Path normalized(Path value, String name) {
            return Objects.requireNonNull(value, name).toAbsolutePath().normalize();
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) return true;
            if (!(other instanceof RuntimeKey)) return false;
            RuntimeKey that = (RuntimeKey) other;
            return materialsDirectory.equals(that.materialsDirectory)
                    && auditFile.equals(that.auditFile)
                    && evaluationTime.equals(that.evaluationTime);
        }

        @Override
        public int hashCode() {
            return Objects.hash(materialsDirectory, auditFile, evaluationTime);
        }
    }
}
