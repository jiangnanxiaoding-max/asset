package com.jason.yang.asset.domain;

import java.time.Instant;
import java.util.Objects;

public final class AddressRiskAssessment {
    private final int riskScore;
    private final RiskCategory category;
    private final Instant assessedAt;
    private final String providerReference;

    public int riskScore() {
        return riskScore;
    }

    public int getRiskScore() {
        return riskScore;
    }

    public RiskCategory category() {
        return category;
    }

    public RiskCategory getCategory() {
        return category;
    }

    public Instant assessedAt() {
        return assessedAt;
    }

    public Instant getAssessedAt() {
        return assessedAt;
    }

    public String providerReference() {
        return providerReference;
    }

    public String getProviderReference() {
        return providerReference;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof AddressRiskAssessment)) return false;
        AddressRiskAssessment that = (AddressRiskAssessment) other;
        return riskScore == that.riskScore
                && java.util.Objects.equals(category, that.category)
                && java.util.Objects.equals(assessedAt, that.assessedAt)
                && java.util.Objects.equals(providerReference, that.providerReference);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(riskScore, category, assessedAt, providerReference);
    }

    @Override
    public String toString() {
        return "AddressRiskAssessment{" + "riskScore=" + riskScore + ", category=" + category + ", assessedAt=" + assessedAt + ", providerReference=" + providerReference + "}";
    }


    public AddressRiskAssessment(int riskScore, RiskCategory category, Instant assessedAt, String providerReference) {
        Objects.requireNonNull(category);
        Objects.requireNonNull(assessedAt);
        providerReference = providerReference == null ? "" : providerReference;
        new RiskScore(riskScore);
    

        this.riskScore = riskScore;

        this.category = category;

        this.assessedAt = assessedAt;

        this.providerReference = providerReference;

    }

    public RiskScore score() {
        return new RiskScore(riskScore);
    }

    public enum RiskCategory {
        CLEAN,
        SANCTIONED,
        MIXER,
        DARKNET,
        UNKNOWN,
        OTHER
    }
}
