package com.jason.yang.asset.domain;

import java.time.Instant;

/** A completed business fact emitted by the triage aggregate. */
public interface DomainEvent {
    OrderId orderId();

    Instant occurredAt();
}
