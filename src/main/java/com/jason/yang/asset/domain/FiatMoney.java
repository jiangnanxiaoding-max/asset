package com.jason.yang.asset.domain;

import java.math.BigDecimal;
import java.util.Objects;

/** Positive fiat value that cannot be compared without an explicit currency. */
public final class FiatMoney {
    private final BigDecimal value;
    private final String currency;

    public BigDecimal value() {
        return value;
    }

    public BigDecimal getValue() {
        return value;
    }

    public String currency() {
        return currency;
    }

    public String getCurrency() {
        return currency;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof FiatMoney)) return false;
        FiatMoney that = (FiatMoney) other;
        return java.util.Objects.equals(value, that.value)
                && java.util.Objects.equals(currency, that.currency);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(value, currency);
    }

    @Override
    public String toString() {
        return "FiatMoney{" + "value=" + value + ", currency=" + currency + "}";
    }


    public FiatMoney(BigDecimal value, String currency) {
        Objects.requireNonNull(value, "value");
        if (value.signum() <= 0) {
            throw new IllegalArgumentException("fiat amount must be positive");
        }
        if (currency == null || currency.trim().isEmpty()) {
            throw new IllegalArgumentException("currency must not be blank");
        }
        currency = currency.toUpperCase();
    

        this.value = value;

        this.currency = currency;

    }

    public boolean isUsd() {
        return "USD".equals(currency);
    }
}
