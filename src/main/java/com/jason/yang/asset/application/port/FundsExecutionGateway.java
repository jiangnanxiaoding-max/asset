package com.jason.yang.asset.application.port;

import com.jason.yang.asset.domain.TriageCase;

/** Isolated funds boundary; the exercise implementation only records simulated requests. */
public interface FundsExecutionGateway {
    ExecutionRecord execute(String idempotencyKey, TriageCase triageCase);final class ExecutionRecord {
    private final Status status;
    private final String externalReference;

    public ExecutionRecord(Status status, String externalReference) {

        this.status = status;
        this.externalReference = externalReference;
    }

    public Status status() {
        return status;
    }

    public Status getStatus() {
        return status;
    }

    public String externalReference() {
        return externalReference;
    }

    public String getExternalReference() {
        return externalReference;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof ExecutionRecord)) return false;
        ExecutionRecord that = (ExecutionRecord) other;
        return java.util.Objects.equals(status, that.status)
                && java.util.Objects.equals(externalReference, that.externalReference);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(status, externalReference);
    }

    @Override
    public String toString() {
        return "ExecutionRecord{" + "status=" + status + ", externalReference=" + externalReference + "}";
    }


    }

    enum Status {
        SUCCEEDED,
        REJECTED,
        STATUS_UNKNOWN
    }
}
