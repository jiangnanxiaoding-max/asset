package com.jason.yang.asset.application.service;

import com.jason.yang.asset.application.DecisionExplanationService;
import com.jason.yang.asset.domain.RuleResult;
import com.jason.yang.asset.domain.TriageDecision;

import java.util.List;
import java.util.stream.Collectors;

/** Deterministic offline explanation generator; its output never grants authorization. */
public final class TemplateDecisionExplanationService implements DecisionExplanationService {
    @Override
    public String explain(TriageDecision decision, List<RuleResult> ruleResults) {
        String failures = ruleResults.stream()
                .filter(result -> !result.passed())
                .map(RuleResult::detail)
                .filter(detail -> !detail.trim().isEmpty())
                .collect(Collectors.joining("；"));

        if (failures.trim().isEmpty()) {
            return "订单 " + decision.orderId().value() + " 的全部强制检查均已通过，可进入受控执行流程。";
        }
        return "订单 " + decision.orderId().value() + " 的处置为 " + decision.disposition()
                + "：" + failures + "。";
    }
}
