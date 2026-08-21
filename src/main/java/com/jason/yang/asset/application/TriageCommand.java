package com.jason.yang.asset.application;

import com.jason.yang.asset.domain.Order;

import java.util.Objects;

/** Use-case command carrying transport audit metadata separately from the domain order. */
public final class TriageCommand {
    private final Order order;
    private final String runId;
    private final String payloadSha256;
    private final long sourcePosition;

    public Order order() {
        return order;
    }

    public Order getOrder() {
        return order;
    }

    public String runId() {
        return runId;
    }

    public String getRunId() {
        return runId;
    }

    public String payloadSha256() {
        return payloadSha256;
    }

    public String getPayloadSha256() {
        return payloadSha256;
    }

    public long sourcePosition() {
        return sourcePosition;
    }

    public long getSourcePosition() {
        return sourcePosition;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof TriageCommand)) return false;
        TriageCommand that = (TriageCommand) other;
        return java.util.Objects.equals(order, that.order)
                && java.util.Objects.equals(runId, that.runId)
                && java.util.Objects.equals(payloadSha256, that.payloadSha256)
                && sourcePosition == that.sourcePosition;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(order, runId, payloadSha256, sourcePosition);
    }

    @Override
    public String toString() {
        return "TriageCommand{" + "order=" + order + ", runId=" + runId + ", payloadSha256=" + payloadSha256 + ", sourcePosition=" + sourcePosition + "}";
    }


    public TriageCommand(Order order, String runId, String payloadSha256, long sourcePosition) {
        Objects.requireNonNull(order);
        runId = runId == null ? "direct" : runId;
        payloadSha256 = payloadSha256 == null ? "" : payloadSha256;
    

        this.order = order;

        this.runId = runId;

        this.payloadSha256 = payloadSha256;

        this.sourcePosition = sourcePosition;

    }

    public static TriageCommand direct(Order order) {
        return new TriageCommand(order, "direct", "", 0);
    }
}
