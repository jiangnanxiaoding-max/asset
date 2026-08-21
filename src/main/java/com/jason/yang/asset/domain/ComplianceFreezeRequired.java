package com.jason.yang.asset.domain;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public final class ComplianceFreezeRequired implements DomainEvent {
    private final OrderId orderId;
    private final List<ReasonCode> reasons;
    private final Instant occurredAt;

    public OrderId orderId() {
        return orderId;
    }

    public OrderId getOrderId() {
        return orderId;
    }

    public List<ReasonCode> reasons() {
        return reasons;
    }

    public List<ReasonCode> getReasons() {
        return reasons;
    }

    public Instant occurredAt() {
        return occurredAt;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof ComplianceFreezeRequired)) return false;
        ComplianceFreezeRequired that = (ComplianceFreezeRequired) other;
        return java.util.Objects.equals(orderId, that.orderId)
                && java.util.Objects.equals(reasons, that.reasons)
                && java.util.Objects.equals(occurredAt, that.occurredAt);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(orderId, reasons, occurredAt);
    }

    @Override
    public String toString() {
        return "ComplianceFreezeRequired{" + "orderId=" + orderId + ", reasons=" + reasons + ", occurredAt=" + occurredAt + "}";
    }


    public ComplianceFreezeRequired(OrderId orderId, List<ReasonCode> reasons, Instant occurredAt) {
        Objects.requireNonNull(orderId);
        reasons = java.util.Collections.unmodifiableList(new java.util.ArrayList<>(reasons));
        Objects.requireNonNull(occurredAt);
    

        this.orderId = orderId;

        this.reasons = reasons;

        this.occurredAt = occurredAt;

    }
}
