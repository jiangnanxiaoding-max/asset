package com.jason.yang.asset.application.port;

import com.jason.yang.asset.domain.DomainEvent;

import java.util.List;

/** Publishes completed domain facts after the aggregate has been durably saved. */
@FunctionalInterface
public interface DomainEventPublisher {
    void publish(List<DomainEvent> events);
}
