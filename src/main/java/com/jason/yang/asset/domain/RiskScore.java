package com.jason.yang.asset.domain;

/** Address risk score constrained to the provider contract range of 0 through 100. */
public final class RiskScore implements Comparable<RiskScore> {
    private final int value;

    public int value() {
        return value;
    }

    public int getValue() {
        return value;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof RiskScore)) return false;
        RiskScore that = (RiskScore) other;
        return value == that.value;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(value);
    }

    @Override
    public String toString() {
        return "RiskScore{" + "value=" + value + "}";
    }


    public RiskScore(int value) {
        if (value < 0 || value > 100) {
            throw new IllegalArgumentException("risk score must be between 0 and 100");
        }
    

        this.value = value;

    }

    @Override
    public int compareTo(RiskScore other) {
        return Integer.compare(value, other.value);
    }

    public boolean atLeast(int threshold) {
        return value >= threshold;
    }
}
