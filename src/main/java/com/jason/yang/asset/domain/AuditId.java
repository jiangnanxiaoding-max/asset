package com.jason.yang.asset.domain;

/** Reference to the immutable audit record required before execution eligibility. */
public final class AuditId {
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
        if (!(other instanceof AuditId)) return false;
        AuditId that = (AuditId) other;
        return java.util.Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(value);
    }

    @Override
    public String toString() {
        return "AuditId{" + "value=" + value + "}";
    }


    public AuditId(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("auditId must not be blank");
        }
    

        this.value = value;

    }
}
