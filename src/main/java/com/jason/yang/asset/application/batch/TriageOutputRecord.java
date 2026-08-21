package com.jason.yang.asset.application.batch;

import com.jason.yang.asset.application.input.InputViolation;
import com.jason.yang.asset.domain.Disposition;
import com.jason.yang.asset.domain.ReasonCode;

import java.time.Instant;
import java.util.List;

public final class TriageOutputRecord {
    private final String contractVersion;
    private final String runId;
    private final long sourcePosition;
    private final String decisionId;
    private final String orderId;
    private final Disposition disposition;
    private final List<ReasonCode> reasonCodes;
    private final boolean fundsMovementAllowed;
    private final String humanExplanation;
    private final String policyVersion;
    private final Instant evaluatedAt;
    private final String auditId;
    private final boolean replayed;
    private final List<InputViolation> inputViolations;

    public String contractVersion() {
        return contractVersion;
    }

    public String getContractVersion() {
        return contractVersion;
    }

    public String runId() {
        return runId;
    }

    public String getRunId() {
        return runId;
    }

    public long sourcePosition() {
        return sourcePosition;
    }

    public long getSourcePosition() {
        return sourcePosition;
    }

    public String decisionId() {
        return decisionId;
    }

    public String getDecisionId() {
        return decisionId;
    }

    public String orderId() {
        return orderId;
    }

    public String getOrderId() {
        return orderId;
    }

    public Disposition disposition() {
        return disposition;
    }

    public Disposition getDisposition() {
        return disposition;
    }

    public List<ReasonCode> reasonCodes() {
        return reasonCodes;
    }

    public List<ReasonCode> getReasonCodes() {
        return reasonCodes;
    }

    public boolean fundsMovementAllowed() {
        return fundsMovementAllowed;
    }

    public boolean getFundsMovementAllowed() {
        return fundsMovementAllowed;
    }

    public String humanExplanation() {
        return humanExplanation;
    }

    public String getHumanExplanation() {
        return humanExplanation;
    }

    public String policyVersion() {
        return policyVersion;
    }

    public String getPolicyVersion() {
        return policyVersion;
    }

    public Instant evaluatedAt() {
        return evaluatedAt;
    }

    public Instant getEvaluatedAt() {
        return evaluatedAt;
    }

    public String auditId() {
        return auditId;
    }

    public String getAuditId() {
        return auditId;
    }

    public boolean replayed() {
        return replayed;
    }

    public boolean getReplayed() {
        return replayed;
    }

    public List<InputViolation> inputViolations() {
        return inputViolations;
    }

    public List<InputViolation> getInputViolations() {
        return inputViolations;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof TriageOutputRecord)) return false;
        TriageOutputRecord that = (TriageOutputRecord) other;
        return java.util.Objects.equals(contractVersion, that.contractVersion)
                && java.util.Objects.equals(runId, that.runId)
                && sourcePosition == that.sourcePosition
                && java.util.Objects.equals(decisionId, that.decisionId)
                && java.util.Objects.equals(orderId, that.orderId)
                && java.util.Objects.equals(disposition, that.disposition)
                && java.util.Objects.equals(reasonCodes, that.reasonCodes)
                && fundsMovementAllowed == that.fundsMovementAllowed
                && java.util.Objects.equals(humanExplanation, that.humanExplanation)
                && java.util.Objects.equals(policyVersion, that.policyVersion)
                && java.util.Objects.equals(evaluatedAt, that.evaluatedAt)
                && java.util.Objects.equals(auditId, that.auditId)
                && replayed == that.replayed
                && java.util.Objects.equals(inputViolations, that.inputViolations);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(contractVersion, runId, sourcePosition, decisionId, orderId, disposition, reasonCodes, fundsMovementAllowed, humanExplanation, policyVersion, evaluatedAt, auditId, replayed, inputViolations);
    }

    @Override
    public String toString() {
        return "TriageOutputRecord{" + "contractVersion=" + contractVersion + ", runId=" + runId + ", sourcePosition=" + sourcePosition + ", decisionId=" + decisionId + ", orderId=" + orderId + ", disposition=" + disposition + ", reasonCodes=" + reasonCodes + ", fundsMovementAllowed=" + fundsMovementAllowed + ", humanExplanation=" + humanExplanation + ", policyVersion=" + policyVersion + ", evaluatedAt=" + evaluatedAt + ", auditId=" + auditId + ", replayed=" + replayed + ", inputViolations=" + inputViolations + "}";
    }


    public TriageOutputRecord(String contractVersion, String runId, long sourcePosition, String decisionId, String orderId, Disposition disposition, List<ReasonCode> reasonCodes, boolean fundsMovementAllowed, String humanExplanation, String policyVersion, Instant evaluatedAt, String auditId, boolean replayed, List<InputViolation> inputViolations) {
        reasonCodes = java.util.Collections.unmodifiableList(new java.util.ArrayList<>(reasonCodes));
        inputViolations = java.util.Collections.unmodifiableList(new java.util.ArrayList<>(inputViolations));
    

        this.contractVersion = contractVersion;

        this.runId = runId;

        this.sourcePosition = sourcePosition;

        this.decisionId = decisionId;

        this.orderId = orderId;

        this.disposition = disposition;

        this.reasonCodes = reasonCodes;

        this.fundsMovementAllowed = fundsMovementAllowed;

        this.humanExplanation = humanExplanation;

        this.policyVersion = policyVersion;

        this.evaluatedAt = evaluatedAt;

        this.auditId = auditId;

        this.replayed = replayed;

        this.inputViolations = inputViolations;

    }
}
