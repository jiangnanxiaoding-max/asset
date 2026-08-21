package com.jason.yang.asset.application.port;

import com.jason.yang.asset.application.model.TriageResult;

import java.util.Optional;

/** Separates order replay idempotency from chain-event idempotency. */
public interface OrderProcessingPort {
    ProcessingClaim claim(String orderId, String payloadSha256, String runId);

    void complete(String orderId, String payloadSha256, TriageResult result);

    void fail(String orderId, String payloadSha256);

    enum Status {
        ACQUIRED,
        ALREADY_COMPLETED_SAME_PAYLOAD,
        ALREADY_RUNNING,
        PAYLOAD_CONFLICT
    }final class ProcessingClaim {
    private final Status status;
    private final Optional<TriageResult> priorResult;

    public Status status() {
        return status;
    }

    public Status getStatus() {
        return status;
    }

    public Optional<TriageResult> priorResult() {
        return priorResult;
    }

    public Optional<TriageResult> getPriorResult() {
        return priorResult;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof ProcessingClaim)) return false;
        ProcessingClaim that = (ProcessingClaim) other;
        return java.util.Objects.equals(status, that.status)
                && java.util.Objects.equals(priorResult, that.priorResult);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(status, priorResult);
    }

    @Override
    public String toString() {
        return "ProcessingClaim{" + "status=" + status + ", priorResult=" + priorResult + "}";
    }


        public ProcessingClaim(Status status, Optional<TriageResult> priorResult) {
            priorResult = priorResult == null ? Optional.empty() : priorResult;
        

            this.status = status;

            this.priorResult = priorResult;

        }
    }
}
