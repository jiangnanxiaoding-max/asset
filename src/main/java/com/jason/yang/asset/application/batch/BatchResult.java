package com.jason.yang.asset.application.batch;

import java.time.Duration;
import java.util.Map;

public final class BatchResult {
    private final String runId;
    private final long total;
    private final long failed;
    private final Map<String, Long> dispositionCounts;
    private final Duration elapsed;

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

    public Duration elapsed() {
        return elapsed;
    }

    public Duration getElapsed() {
        return elapsed;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof BatchResult)) return false;
        BatchResult that = (BatchResult) other;
        return java.util.Objects.equals(runId, that.runId)
                && total == that.total
                && failed == that.failed
                && java.util.Objects.equals(dispositionCounts, that.dispositionCounts)
                && java.util.Objects.equals(elapsed, that.elapsed);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(runId, total, failed, dispositionCounts, elapsed);
    }

    @Override
    public String toString() {
        return "BatchResult{" + "runId=" + runId + ", total=" + total + ", failed=" + failed + ", dispositionCounts=" + dispositionCounts + ", elapsed=" + elapsed + "}";
    }


    public BatchResult(String runId, long total, long failed, Map<String, Long> dispositionCounts, Duration elapsed) {
        dispositionCounts = java.util.Collections.unmodifiableMap(new java.util.HashMap<>(dispositionCounts));
    

        this.runId = runId;

        this.total = total;

        this.failed = failed;

        this.dispositionCounts = dispositionCounts;

        this.elapsed = elapsed;

    }
}
