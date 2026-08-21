package com.jason.yang.asset.application;

import com.jason.yang.asset.domain.Order;
import com.jason.yang.asset.application.model.TriageResult;

/** Inbound use-case contract for triaging one already-normalized order. */
public interface TriageOrderUseCase {
    TriageResult triage(TriageCommand command);

    default TriageResult triage(Order order) {
        return triage(TriageCommand.direct(order));
    }
}
