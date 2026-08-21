package com.jason.yang.asset.application.batch;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Objects;

public final class BatchCommand {
    private final Path ordersFile;
    private final Path outputFile;
    private final String runId;
    private final Instant evaluationTime;
    private final int maxConcurrency;

    public Path ordersFile() {
        return ordersFile;
    }

    public Path getOrdersFile() {
        return ordersFile;
    }

    public Path outputFile() {
        return outputFile;
    }

    public Path getOutputFile() {
        return outputFile;
    }

    public String runId() {
        return runId;
    }

    public String getRunId() {
        return runId;
    }

    public Instant evaluationTime() {
        return evaluationTime;
    }

    public Instant getEvaluationTime() {
        return evaluationTime;
    }

    public int maxConcurrency() {
        return maxConcurrency;
    }

    public int getMaxConcurrency() {
        return maxConcurrency;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof BatchCommand)) return false;
        BatchCommand that = (BatchCommand) other;
        return java.util.Objects.equals(ordersFile, that.ordersFile)
                && java.util.Objects.equals(outputFile, that.outputFile)
                && java.util.Objects.equals(runId, that.runId)
                && java.util.Objects.equals(evaluationTime, that.evaluationTime)
                && maxConcurrency == that.maxConcurrency;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(ordersFile, outputFile, runId, evaluationTime, maxConcurrency);
    }

    @Override
    public String toString() {
        return "BatchCommand{" + "ordersFile=" + ordersFile + ", outputFile=" + outputFile + ", runId=" + runId + ", evaluationTime=" + evaluationTime + ", maxConcurrency=" + maxConcurrency + "}";
    }


    public BatchCommand(Path ordersFile, Path outputFile, String runId, Instant evaluationTime, int maxConcurrency) {
        Objects.requireNonNull(ordersFile);
        Objects.requireNonNull(outputFile);
        Objects.requireNonNull(runId);
        Objects.requireNonNull(evaluationTime);
        if (maxConcurrency < 1) {
            throw new IllegalArgumentException("maxConcurrency must be positive");
        }
    

        this.ordersFile = ordersFile;

        this.outputFile = outputFile;

        this.runId = runId;

        this.evaluationTime = evaluationTime;

        this.maxConcurrency = maxConcurrency;

    }
}
