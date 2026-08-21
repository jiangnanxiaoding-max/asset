package com.jason.yang.asset.application.port;

import com.jason.yang.asset.domain.TriageDecision;

/** Idempotently opens non-funds cases after the decision audit has succeeded. */
public interface CaseManagementPort {
    String openCase(String idempotencyKey, String caseType, TriageDecision decision);
}
