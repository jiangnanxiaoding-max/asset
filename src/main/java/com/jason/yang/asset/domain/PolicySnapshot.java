package com.jason.yang.asset.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

public final class PolicySnapshot {
    private final String version;
    private final Instant evaluationTime;
    private final BigDecimal quoteSlippageTolerance;
    private final BigDecimal travelRuleThresholdUsd;

    public String version() {
        return version;
    }

    public String getVersion() {
        return version;
    }

    public Instant evaluationTime() {
        return evaluationTime;
    }

    public Instant getEvaluationTime() {
        return evaluationTime;
    }

    public BigDecimal quoteSlippageTolerance() {
        return quoteSlippageTolerance;
    }

    public BigDecimal getQuoteSlippageTolerance() {
        return quoteSlippageTolerance;
    }

    public BigDecimal travelRuleThresholdUsd() {
        return travelRuleThresholdUsd;
    }

    public BigDecimal getTravelRuleThresholdUsd() {
        return travelRuleThresholdUsd;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof PolicySnapshot)) return false;
        PolicySnapshot that = (PolicySnapshot) other;
        return java.util.Objects.equals(version, that.version)
                && java.util.Objects.equals(evaluationTime, that.evaluationTime)
                && java.util.Objects.equals(quoteSlippageTolerance, that.quoteSlippageTolerance)
                && java.util.Objects.equals(travelRuleThresholdUsd, that.travelRuleThresholdUsd);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(version, evaluationTime, quoteSlippageTolerance, travelRuleThresholdUsd);
    }

    @Override
    public String toString() {
        return "PolicySnapshot{" + "version=" + version + ", evaluationTime=" + evaluationTime + ", quoteSlippageTolerance=" + quoteSlippageTolerance + ", travelRuleThresholdUsd=" + travelRuleThresholdUsd + "}";
    }


    public PolicySnapshot(String version, Instant evaluationTime, BigDecimal quoteSlippageTolerance, BigDecimal travelRuleThresholdUsd) {
        Objects.requireNonNull(version);
        Objects.requireNonNull(evaluationTime);
        Objects.requireNonNull(quoteSlippageTolerance);
        Objects.requireNonNull(travelRuleThresholdUsd);
        if (quoteSlippageTolerance.signum() < 0) {
            throw new IllegalArgumentException("slippage tolerance must not be negative");
        }
        if (travelRuleThresholdUsd.signum() <= 0) {
            throw new IllegalArgumentException("travel rule threshold must be positive");
        }
    

        this.version = version;

        this.evaluationTime = evaluationTime;

        this.quoteSlippageTolerance = quoteSlippageTolerance;

        this.travelRuleThresholdUsd = travelRuleThresholdUsd;

    }
}
