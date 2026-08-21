package com.jason.yang.asset.application;

import com.jason.yang.asset.domain.RuleResult;
import com.jason.yang.asset.domain.TriageDecision;

import java.util.List;

/** Produces non-authoritative human-readable text from an already-final decision. */
public interface DecisionExplanationService {
    String explain(TriageDecision decision, List<RuleResult> ruleResults);
}
