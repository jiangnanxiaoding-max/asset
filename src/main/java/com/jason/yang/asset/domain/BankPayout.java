package com.jason.yang.asset.domain;

import java.math.BigDecimal;
import java.util.Objects;

public final class BankPayout {
    private final String bankAccountName;
    private final String currency;
    private final BigDecimal amount;

    public String bankAccountName() {
        return bankAccountName;
    }

    public String getBankAccountName() {
        return bankAccountName;
    }

    public String currency() {
        return currency;
    }

    public String getCurrency() {
        return currency;
    }

    public BigDecimal amount() {
        return amount;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof BankPayout)) return false;
        BankPayout that = (BankPayout) other;
        return java.util.Objects.equals(bankAccountName, that.bankAccountName)
                && java.util.Objects.equals(currency, that.currency)
                && java.util.Objects.equals(amount, that.amount);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(bankAccountName, currency, amount);
    }

    @Override
    public String toString() {
        return "BankPayout{" + "bankAccountName=" + bankAccountName + ", currency=" + currency + ", amount=" + amount + "}";
    }


    public BankPayout(String bankAccountName, String currency, BigDecimal amount) {
        Objects.requireNonNull(bankAccountName);
        Objects.requireNonNull(currency);
        Objects.requireNonNull(amount);
        new FiatMoney(amount, currency);
    

        this.bankAccountName = bankAccountName;

        this.currency = currency;

        this.amount = amount;

    }

    public FiatMoney money() {
        return new FiatMoney(amount, currency);
    }
}
