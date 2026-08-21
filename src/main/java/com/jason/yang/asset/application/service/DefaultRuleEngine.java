package com.jason.yang.asset.application.service;

import com.jason.yang.asset.application.RuleEngine;
import com.jason.yang.asset.domain.policy.TriageRule;
import com.jason.yang.asset.domain.InvestigationFacts;
import com.jason.yang.asset.domain.PolicySnapshot;
import com.jason.yang.asset.domain.RuleResult;

import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;

/** Application-side evaluator that invokes every registered pure domain policy. */
public final class DefaultRuleEngine implements RuleEngine {
    private final List<TriageRule> rules;

    public DefaultRuleEngine(List<TriageRule> rules) {
        this.rules = java.util.Collections.unmodifiableList(new ArrayList<TriageRule>(rules));
    }

    @Override
    public List<RuleResult> evaluateAll(InvestigationFacts facts, PolicySnapshot policy) {
        return rules.stream()
                .map(rule -> rule.evaluate(facts, policy))
                .collect(Collectors.toList());
    }
}
