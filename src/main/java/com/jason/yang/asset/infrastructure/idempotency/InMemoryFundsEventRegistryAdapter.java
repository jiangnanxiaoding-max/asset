package com.jason.yang.asset.infrastructure.idempotency;

import com.jason.yang.asset.application.port.FundsEventRegistryPort;
import com.jason.yang.asset.domain.DuplicateAssessment;
import com.jason.yang.asset.domain.LookupResult;
import com.jason.yang.asset.domain.OffRampOrder;

import java.util.concurrent.ConcurrentHashMap;

/** Atomic process-local chain-event claim registry used by the offline batch runner. */
public final class InMemoryFundsEventRegistryAdapter implements FundsEventRegistryPort {
    private final ConcurrentHashMap<String, String> owners = new ConcurrentHashMap<>();

    @Override
    public LookupResult<DuplicateAssessment> assess(OffRampOrder order) {
        String eventKey = order.deposit().eventKey();
        String owner = owners.putIfAbsent(eventKey, order.orderId());
        if (owner == null || owner.equals(order.orderId())) {
            return LookupResult.found(
                    new DuplicateAssessment(DuplicateAssessment.Status.NEW, order.orderId()),
                    "funds-event:new:" + eventKey
            );
        }
        return LookupResult.found(
                new DuplicateAssessment(DuplicateAssessment.Status.ALREADY_CREDITED, owner),
                "funds-event:duplicate:" + eventKey
        );
    }
}
