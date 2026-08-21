package com.jason.yang.asset.domain;

import java.util.Objects;
import java.util.Optional;

public final class RuleResult {
    private final String ruleId;
    private final boolean passed;
    private final Optional<Disposition> proposedDisposition;
    private final Optional<ReasonCode> reasonCode;
    private final String detail;

    public String ruleId() {
        return ruleId;
    }

    public String getRuleId() {
        return ruleId;
    }

    public boolean passed() {
        return passed;
    }

    public boolean getPassed() {
        return passed;
    }

    public Optional<Disposition> proposedDisposition() {
        return proposedDisposition;
    }

    public Optional<Disposition> getProposedDisposition() {
        return proposedDisposition;
    }

    public Optional<ReasonCode> reasonCode() {
        return reasonCode;
    }

    public Optional<ReasonCode> getReasonCode() {
        return reasonCode;
    }

    public String detail() {
        return detail;
    }

    public String getDetail() {
        return detail;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof RuleResult)) return false;
        RuleResult that = (RuleResult) other;
        return java.util.Objects.equals(ruleId, that.ruleId)
                && passed == that.passed
                && java.util.Objects.equals(proposedDisposition, that.proposedDisposition)
                && java.util.Objects.equals(reasonCode, that.reasonCode)
                && java.util.Objects.equals(detail, that.detail);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(ruleId, passed, proposedDisposition, reasonCode, detail);
    }

    @Override
    public String toString() {
        return "RuleResult{" + "ruleId=" + ruleId + ", passed=" + passed + ", proposedDisposition=" + proposedDisposition + ", reasonCode=" + reasonCode + ", detail=" + detail + "}";
    }


    public RuleResult(String ruleId, boolean passed, Optional<Disposition> proposedDisposition, Optional<ReasonCode> reasonCode, String detail) {
        Objects.requireNonNull(ruleId);
        proposedDisposition = proposedDisposition == null ? Optional.empty() : proposedDisposition;
        reasonCode = reasonCode == null ? Optional.empty() : reasonCode;
        detail = detail == null ? "" : detail;
    

        this.ruleId = ruleId;

        this.passed = passed;

        this.proposedDisposition = proposedDisposition;

        this.reasonCode = reasonCode;

        this.detail = detail;

    }

    public static RuleResult pass(String ruleId) {
        return new RuleResult(ruleId, true, Optional.empty(), Optional.empty(), "passed");
    }

    public static RuleResult fail(
            String ruleId,
            Disposition disposition,
            ReasonCode reasonCode,
            String detail
    ) {
        return new RuleResult(
                ruleId,
                false,
                Optional.of(disposition),
                Optional.of(reasonCode),
                detail
        );
    }
}
