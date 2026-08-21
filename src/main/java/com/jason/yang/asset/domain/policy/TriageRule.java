package com.jason.yang.asset.domain.policy;

import com.jason.yang.asset.domain.InvestigationFacts;
import com.jason.yang.asset.domain.PolicySnapshot;
import com.jason.yang.asset.domain.RuleResult;

/** Pure domain policy rule; it evaluates known facts and never performs I/O. */
public interface TriageRule {
    String id();

    RuleResult evaluate(InvestigationFacts facts, PolicySnapshot policy);
}
