package com.jason.yang.asset.infrastructure.idempotency;

import com.jason.yang.asset.application.model.TriageResult;
import com.jason.yang.asset.application.port.OrderProcessingPort;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** Atomic order-level claim registry for deterministic replay within one batch process. */
public final class InMemoryOrderProcessingAdapter implements OrderProcessingPort {
    private final ConcurrentHashMap<String, Entry> entries = new ConcurrentHashMap<>();

    @Override
    public ProcessingClaim claim(String orderId, String payloadSha256, String runId) {
        Entry candidate = new Entry(payloadSha256, State.RUNNING, null);
        Entry existing = entries.putIfAbsent(orderId, candidate);
        if (existing == null) {
            return new ProcessingClaim(Status.ACQUIRED, Optional.empty());
        }
        if (!existing.payloadSha256.equals(payloadSha256)) {
            return new ProcessingClaim(Status.PAYLOAD_CONFLICT, Optional.empty());
        }
        if (existing.state == State.COMPLETED) {
            return new ProcessingClaim(Status.ALREADY_COMPLETED_SAME_PAYLOAD,
                    Optional.ofNullable(existing.result));
        }
        return new ProcessingClaim(Status.ALREADY_RUNNING, Optional.empty());
    }

    @Override
    public void complete(String orderId, String payloadSha256, TriageResult result) {
        entries.compute(orderId, (ignored, current) -> {
            verifyOwner(current, payloadSha256);
            return new Entry(payloadSha256, State.COMPLETED, result);
        });
    }

    @Override
    public void fail(String orderId, String payloadSha256) {
        entries.computeIfPresent(orderId, (ignored, current) ->
                current.payloadSha256.equals(payloadSha256) ? null : current);
    }

    private void verifyOwner(Entry current, String payloadSha256) {
        if (current == null || !current.payloadSha256.equals(payloadSha256)) {
            throw new IllegalStateException("Order claim ownership changed");
        }
    }

    private enum State { RUNNING, COMPLETED }

    private static final class Entry {
    private final String payloadSha256;
    private final State state;
    private final TriageResult result;

    public Entry(String payloadSha256, State state, TriageResult result) {

        this.payloadSha256 = payloadSha256;
        this.state = state;
        this.result = result;
    }

    public String payloadSha256() {
        return payloadSha256;
    }

    public String getPayloadSha256() {
        return payloadSha256;
    }

    public State state() {
        return state;
    }

    public State getState() {
        return state;
    }

    public TriageResult result() {
        return result;
    }

    public TriageResult getResult() {
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof Entry)) return false;
        Entry that = (Entry) other;
        return java.util.Objects.equals(payloadSha256, that.payloadSha256)
                && java.util.Objects.equals(state, that.state)
                && java.util.Objects.equals(result, that.result);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(payloadSha256, state, result);
    }

    @Override
    public String toString() {
        return "Entry{" + "payloadSha256=" + payloadSha256 + ", state=" + state + ", result=" + result + "}";
    }


    }
}
