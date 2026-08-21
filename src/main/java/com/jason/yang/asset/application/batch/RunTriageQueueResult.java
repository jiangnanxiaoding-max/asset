package com.jason.yang.asset.application.batch;

import java.nio.file.Path;
import java.util.Objects;

/** Result of one queue execution, including the files produced by the application service. */
public final class RunTriageQueueResult {
    private final BatchResult batchResult;
    private final Path outputFile;
    private final Path auditFile;

    public RunTriageQueueResult(BatchResult batchResult, Path outputFile, Path auditFile) {
        this.batchResult = Objects.requireNonNull(batchResult, "batchResult");
        this.outputFile = normalized(outputFile, "outputFile");
        this.auditFile = normalized(auditFile, "auditFile");
    }

    public BatchResult batchResult() {
        return batchResult;
    }

    public BatchResult getBatchResult() {
        return batchResult;
    }

    public Path outputFile() {
        return outputFile;
    }

    public Path getOutputFile() {
        return outputFile;
    }

    public Path auditFile() {
        return auditFile;
    }

    public Path getAuditFile() {
        return auditFile;
    }

    private static Path normalized(Path value, String name) {
        return Objects.requireNonNull(value, name).toAbsolutePath().normalize();
    }
}
