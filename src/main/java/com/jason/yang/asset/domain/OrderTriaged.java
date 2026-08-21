package com.jason.yang.asset.domain;

import java.time.Instant;
import java.util.Objects;

public final class OrderTriaged implements DomainEvent {
    private final OrderId orderId;
    private final Disposition disposition;
    private final Instant occurredAt;

    public OrderId orderId() {
        return orderId;
    }

    public OrderId getOrderId() {
        return orderId;
    }

    public Disposition disposition() {
        return disposition;
    }

    public Disposition getDisposition() {
        return disposition;
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
        if (!(other instanceof OrderTriaged)) return false;
        OrderTriaged that = (OrderTriaged) other;
        return java.util.Objects.equals(orderId, that.orderId)
                && java.util.Objects.equals(disposition, that.disposition)
                && java.util.Objects.equals(occurredAt, that.occurredAt);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(orderId, disposition, occurredAt);
    }

    @Override
    public String toString() {
        return "OrderTriaged{" + "orderId=" + orderId + ", disposition=" + disposition + ", occurredAt=" + occurredAt + "}";
    }


    public OrderTriaged(OrderId orderId, Disposition disposition, Instant occurredAt) {
        Objects.requireNonNull(orderId);
        Objects.requireNonNull(disposition);
        Objects.requireNonNull(occurredAt);
    

        this.orderId = orderId;

        this.disposition = disposition;

        this.occurredAt = occurredAt;

    }
}
