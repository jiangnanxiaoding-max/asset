package com.jason.yang.asset.domain;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public final class TriageDecision {
    private final DecisionId decisionId;
    private final OrderId orderId;
    private final Disposition disposition;
    private final List<ReasonCode> reasonCodes;
    private final boolean fundsMovementAllowed;
    private final String policyVersion;
    private final Instant evaluatedAt;

    public DecisionId decisionId() {
        return decisionId;
    }

    public DecisionId getDecisionId() {
        return decisionId;
    }

    public OrderId orderId() {
        return orderId;
    }

    public OrderId getOrderId() {
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

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof TriageDecision)) return false;
        TriageDecision that = (TriageDecision) other;
        return java.util.Objects.equals(decisionId, that.decisionId)
                && java.util.Objects.equals(orderId, that.orderId)
                && java.util.Objects.equals(disposition, that.disposition)
                && java.util.Objects.equals(reasonCodes, that.reasonCodes)
                && fundsMovementAllowed == that.fundsMovementAllowed
                && java.util.Objects.equals(policyVersion, that.policyVersion)
                && java.util.Objects.equals(evaluatedAt, that.evaluatedAt);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(decisionId, orderId, disposition, reasonCodes, fundsMovementAllowed, policyVersion, evaluatedAt);
    }

    @Override
    public String toString() {
        return "TriageDecision{" + "decisionId=" + decisionId + ", orderId=" + orderId + ", disposition=" + disposition + ", reasonCodes=" + reasonCodes + ", fundsMovementAllowed=" + fundsMovementAllowed + ", policyVersion=" + policyVersion + ", evaluatedAt=" + evaluatedAt + "}";
    }


    public TriageDecision(DecisionId decisionId, OrderId orderId, Disposition disposition, List<ReasonCode> reasonCodes, boolean fundsMovementAllowed, String policyVersion, Instant evaluatedAt) {
        Objects.requireNonNull(decisionId);
        Objects.requireNonNull(orderId);
        Objects.requireNonNull(disposition);
        reasonCodes = java.util.Collections.unmodifiableList(new java.util.ArrayList<>(reasonCodes));
        Objects.requireNonNull(policyVersion);
        Objects.requireNonNull(evaluatedAt);
        if (fundsMovementAllowed && disposition != Disposition.AUTO_COMPLETE) {
            throw new IllegalArgumentException("Only AUTO_COMPLETE may allow funds movement");
        }
    

        this.decisionId = decisionId;

        this.orderId = orderId;

        this.disposition = disposition;

        this.reasonCodes = reasonCodes;

        this.fundsMovementAllowed = fundsMovementAllowed;

        this.policyVersion = policyVersion;

        this.evaluatedAt = evaluatedAt;

    }

    public TriageDecision(
            String decisionId,
            OrderId orderId,
            Disposition disposition,
            List<ReasonCode> reasonCodes,
            boolean fundsMovementAllowed,
            String policyVersion,
            Instant evaluatedAt
    ) {
        this(
                new DecisionId(decisionId),
                orderId,
                disposition,
                reasonCodes,
                fundsMovementAllowed,
                policyVersion,
                evaluatedAt
        );
    }
}
