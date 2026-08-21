package com.jason.yang.asset.infrastructure.event;

import com.jason.yang.asset.application.port.DomainEventPublisher;
import com.jason.yang.asset.domain.ComplianceFreezeRequired;
import com.jason.yang.asset.domain.DomainEvent;
import com.jason.yang.asset.domain.OrderTriaged;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/** Offline event publisher that records only event type and business identity. */
public final class LoggingDomainEventPublisher implements DomainEventPublisher {
    private static final Logger log = LoggerFactory.getLogger(LoggingDomainEventPublisher.class);

    @Override
    public void publish(List<DomainEvent> events) {
        for (DomainEvent event : events) {
            if (event instanceof ComplianceFreezeRequired) {
                ComplianceFreezeRequired freeze = (ComplianceFreezeRequired) event;
                log.warn(
                        "compliance freeze event orderId={} reasonCount={}",
                        freeze.orderId().value(), freeze.reasons().size());
            } else if (event instanceof OrderTriaged) {
                OrderTriaged triaged = (OrderTriaged) event;
                log.info(
                        "order triaged event orderId={} disposition={}",
                        triaged.orderId().value(), triaged.disposition());
            }
        }
    }
}
