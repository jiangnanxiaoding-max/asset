package com.jason.yang.asset.application.port;

import com.jason.yang.asset.domain.InvestigationFacts;
import com.jason.yang.asset.domain.RuleResult;
import com.jason.yang.asset.domain.TriageDecision;
import com.jason.yang.asset.application.model.AgentRunTrace;

import java.util.List;
import java.util.Optional;

/** Appends the complete decision evidence before any execution can become eligible. */
public interface DecisionAuditPort {
    String append(
            TriageDecision decision,
            InvestigationFacts facts,
            List<RuleResult> ruleResults,
            String explanation
    );

    default String append(
            AuditContext context,
            TriageDecision decision,
            InvestigationFacts facts,
            List<RuleResult> ruleResults,
            String explanation
    ) {
        return append(decision, facts, ruleResults, explanation);
    }final class AuditContext {
    private final String runId;
    private final String payloadSha256;
    private final long sourcePosition;
    private final AgentRunTrace agentRunTrace;

    public AuditContext(String runId, String payloadSha256, long sourcePosition) {

        this(runId, payloadSha256, sourcePosition, null);
    }

    public AuditContext(String runId, String payloadSha256, long sourcePosition, AgentRunTrace agentRunTrace) {

        this.runId = runId;
        this.payloadSha256 = payloadSha256;
        this.sourcePosition = sourcePosition;
        this.agentRunTrace = agentRunTrace;
    }

    public String runId() {
        return runId;
    }

    public String getRunId() {
        return runId;
    }

    public String payloadSha256() {
        return payloadSha256;
    }

    public String getPayloadSha256() {
        return payloadSha256;
    }

    public long sourcePosition() {
        return sourcePosition;
    }

    public long getSourcePosition() {
        return sourcePosition;
    }

    public Optional<AgentRunTrace> agentRunTrace() {
        return Optional.ofNullable(agentRunTrace);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof AuditContext)) return false;
        AuditContext that = (AuditContext) other;
        return java.util.Objects.equals(runId, that.runId)
                && java.util.Objects.equals(payloadSha256, that.payloadSha256)
                && sourcePosition == that.sourcePosition
                && java.util.Objects.equals(agentRunTrace, that.agentRunTrace);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(runId, payloadSha256, sourcePosition, agentRunTrace);
    }

    @Override
    public String toString() {
        return "AuditContext{" + "runId=" + runId + ", payloadSha256=" + payloadSha256 + ", sourcePosition=" + sourcePosition + "}";
    }


    }
}
