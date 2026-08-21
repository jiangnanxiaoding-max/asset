package com.jason.yang.asset.infrastructure.action;

import com.jason.yang.asset.application.port.FundsExecutionGateway;
import com.jason.yang.asset.domain.TriageCase;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Simulation-only gateway that independently rejects every non-eligible aggregate. */
public final class RecordingFundsExecutionGateway implements FundsExecutionGateway {
    private final ConcurrentHashMap<String, ExecutionRecord> executions = new ConcurrentHashMap<>();

    @Override
    public ExecutionRecord execute(String idempotencyKey, TriageCase triageCase) {
        if (!triageCase.fundsMovementEligible()) {
            return new ExecutionRecord(Status.REJECTED, "");
        }
        return executions.computeIfAbsent(idempotencyKey,
                ignored -> new ExecutionRecord(Status.SUCCEEDED, "SIM-" + UUID.randomUUID()));
    }
}
