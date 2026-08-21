package com.jason.yang.asset.application.port;

import com.jason.yang.asset.application.model.SideEffectSummary;
import com.jason.yang.asset.domain.TriageCase;

/** Applies audited non-domain side effects according to the selected execution mode. */
public interface PostDecisionActionPort {
    SideEffectSummary handle(TriageCase triageCase);
}
