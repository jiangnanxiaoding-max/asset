package com.jason.yang.asset.application.model;

import com.jason.yang.asset.domain.RuleResult;
import com.jason.yang.asset.domain.TriageDecision;

import java.util.List;

public final class TriageResult {
    private final TriageDecision decision;
    private final String explanation;
    private final String auditId;
    private final List<RuleResult> ruleResults;
    private final SideEffectSummary sideEffects;

    public TriageDecision decision() {
        return decision;
    }

    public TriageDecision getDecision() {
        return decision;
    }

    public String explanation() {
        return explanation;
    }

    public String getExplanation() {
        return explanation;
    }

    public String auditId() {
        return auditId;
    }

    public String getAuditId() {
        return auditId;
    }

    public List<RuleResult> ruleResults() {
        return ruleResults;
    }

    public List<RuleResult> getRuleResults() {
        return ruleResults;
    }

    public SideEffectSummary sideEffects() {
        return sideEffects;
    }

    public SideEffectSummary getSideEffects() {
        return sideEffects;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof TriageResult)) return false;
        TriageResult that = (TriageResult) other;
        return java.util.Objects.equals(decision, that.decision)
                && java.util.Objects.equals(explanation, that.explanation)
                && java.util.Objects.equals(auditId, that.auditId)
                && java.util.Objects.equals(ruleResults, that.ruleResults)
                && java.util.Objects.equals(sideEffects, that.sideEffects);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(decision, explanation, auditId, ruleResults, sideEffects);
    }

    @Override
    public String toString() {
        return "TriageResult{" + "decision=" + decision + ", explanation=" + explanation + ", auditId=" + auditId + ", ruleResults=" + ruleResults + ", sideEffects=" + sideEffects + "}";
    }


    public TriageResult(TriageDecision decision, String explanation, String auditId, List<RuleResult> ruleResults, SideEffectSummary sideEffects) {
        ruleResults = java.util.Collections.unmodifiableList(new java.util.ArrayList<>(ruleResults));
        sideEffects = sideEffects == null ? SideEffectSummary.none() : sideEffects;
    

        this.decision = decision;

        this.explanation = explanation;

        this.auditId = auditId;

        this.ruleResults = ruleResults;

        this.sideEffects = sideEffects;

    }
}
