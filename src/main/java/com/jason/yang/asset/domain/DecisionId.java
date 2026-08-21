package com.jason.yang.asset.domain;

/** Stable identity for an immutable policy decision. */
public final class DecisionId {
    private final String value;

    public String value() {
        return value;
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof DecisionId)) return false;
        DecisionId that = (DecisionId) other;
        return java.util.Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(value);
    }

    @Override
    public String toString() {
        return "DecisionId{" + "value=" + value + "}";
    }


    public DecisionId(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("decisionId must not be blank");
        }
    

        this.value = value;

    }
}
