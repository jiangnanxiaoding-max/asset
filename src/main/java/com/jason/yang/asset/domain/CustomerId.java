package com.jason.yang.asset.domain;

/** Stable identity used to reference the customer aggregate without embedding it. */
public final class CustomerId {
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
        if (!(other instanceof CustomerId)) return false;
        CustomerId that = (CustomerId) other;
        return java.util.Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(value);
    }

    @Override
    public String toString() {
        return "CustomerId{" + "value=" + value + "}";
    }


    public CustomerId(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("customerId must not be blank");
        }
    

        this.value = value;

    }
}
