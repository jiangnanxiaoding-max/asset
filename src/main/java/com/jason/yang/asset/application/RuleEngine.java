package com.jason.yang.asset.application;

import com.jason.yang.asset.domain.InvestigationFacts;
import com.jason.yang.asset.domain.PolicySnapshot;
import com.jason.yang.asset.domain.RuleResult;

import java.util.List;

/** Evaluates every applicable domain policy so severe risks are never hidden by first-match logic. */
public interface RuleEngine {
    List<RuleResult> evaluateAll(InvestigationFacts facts, PolicySnapshot policy);
}
