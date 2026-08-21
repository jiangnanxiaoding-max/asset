package com.jason.yang.asset.infrastructure.action;

import com.jason.yang.asset.application.port.CaseManagementPort;
import com.jason.yang.asset.domain.TriageDecision;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Idempotent in-memory case recorder used instead of a real ticketing system. */
public final class RecordingCaseManagementAdapter implements CaseManagementPort {
    private final ConcurrentHashMap<String, String> cases = new ConcurrentHashMap<>();

    @Override
    public String openCase(String idempotencyKey, String caseType, TriageDecision decision) {
        return cases.computeIfAbsent(idempotencyKey,
                ignored -> "CASE-" + UUID.randomUUID());
    }
}
