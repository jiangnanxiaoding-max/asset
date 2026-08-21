package com.jason.yang.asset.domain;

/** Identifies one business order and rejects blank identities at the domain boundary. */
public final class OrderId {
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
        if (!(other instanceof OrderId)) return false;
        OrderId that = (OrderId) other;
        return java.util.Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(value);
    }

    @Override
    public String toString() {
        return "OrderId{" + "value=" + value + "}";
    }


    public OrderId(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("orderId must not be blank");
        }
    

        this.value = value;

    }
}
