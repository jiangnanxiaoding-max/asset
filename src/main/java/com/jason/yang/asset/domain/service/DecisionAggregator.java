package com.jason.yang.asset.domain.service;

import com.jason.yang.asset.domain.Order;
import com.jason.yang.asset.domain.PolicySnapshot;
import com.jason.yang.asset.domain.RuleResult;
import com.jason.yang.asset.domain.TriageDecision;

import java.util.List;

/** Selects the most conservative disposition from all domain policy results. */
public interface DecisionAggregator {
    TriageDecision aggregate(Order order, List<RuleResult> ruleResults, PolicySnapshot policy);
}
