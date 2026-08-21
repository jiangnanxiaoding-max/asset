package com.jason.yang.asset.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

public final class OffRampOrder implements Order {
    private final String orderId;
    private final String customerId;
    private final String asset;
    private final String network;
    private final BigDecimal quotedCryptoAmount;
    private final Instant quoteExpiresAt;
    private final DepositReference deposit;
    private final BankPayout payout;
    private final CounterpartyInfo counterparty;
    private final String customerNote;

    public String orderId() {
        return orderId;
    }

    public String getOrderId() {
        return orderId;
    }

    public String customerId() {
        return customerId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public String asset() {
        return asset;
    }

    public String getAsset() {
        return asset;
    }

    public String network() {
        return network;
    }

    public String getNetwork() {
        return network;
    }

    public BigDecimal quotedCryptoAmount() {
        return quotedCryptoAmount;
    }

    public BigDecimal getQuotedCryptoAmount() {
        return quotedCryptoAmount;
    }

    public Instant quoteExpiresAt() {
        return quoteExpiresAt;
    }

    public Instant getQuoteExpiresAt() {
        return quoteExpiresAt;
    }

    public DepositReference deposit() {
        return deposit;
    }

    public DepositReference getDeposit() {
        return deposit;
    }

    public BankPayout payout() {
        return payout;
    }

    public BankPayout getPayout() {
        return payout;
    }

    public CounterpartyInfo counterparty() {
        return counterparty;
    }

    public CounterpartyInfo getCounterparty() {
        return counterparty;
    }

    public String customerNote() {
        return customerNote;
    }

    public String getCustomerNote() {
        return customerNote;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof OffRampOrder)) return false;
        OffRampOrder that = (OffRampOrder) other;
        return java.util.Objects.equals(orderId, that.orderId)
                && java.util.Objects.equals(customerId, that.customerId)
                && java.util.Objects.equals(asset, that.asset)
                && java.util.Objects.equals(network, that.network)
                && java.util.Objects.equals(quotedCryptoAmount, that.quotedCryptoAmount)
                && java.util.Objects.equals(quoteExpiresAt, that.quoteExpiresAt)
                && java.util.Objects.equals(deposit, that.deposit)
                && java.util.Objects.equals(payout, that.payout)
                && java.util.Objects.equals(counterparty, that.counterparty)
                && java.util.Objects.equals(customerNote, that.customerNote);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(orderId, customerId, asset, network, quotedCryptoAmount, quoteExpiresAt, deposit, payout, counterparty, customerNote);
    }

    @Override
    public String toString() {
        return "OffRampOrder{" + "orderId=" + orderId + ", customerId=" + customerId + ", asset=" + asset + ", network=" + network + ", quotedCryptoAmount=" + quotedCryptoAmount + ", quoteExpiresAt=" + quoteExpiresAt + ", deposit=" + deposit + ", payout=" + payout + ", counterparty=" + counterparty + ", customerNote=" + customerNote + "}";
    }


    public OffRampOrder(String orderId, String customerId, String asset, String network, BigDecimal quotedCryptoAmount, Instant quoteExpiresAt, DepositReference deposit, BankPayout payout, CounterpartyInfo counterparty, String customerNote) {
        Objects.requireNonNull(orderId);
        Objects.requireNonNull(customerId);
        Objects.requireNonNull(asset);
        Objects.requireNonNull(network);
        Objects.requireNonNull(quotedCryptoAmount);
        Objects.requireNonNull(quoteExpiresAt);
        Objects.requireNonNull(deposit);
        Objects.requireNonNull(payout);
        counterparty = counterparty == null ? CounterpartyInfo.unknown() : counterparty;
        customerNote = customerNote == null ? "" : customerNote;
        new OrderId(orderId);
        new CustomerId(customerId);
        new AssetNetwork(asset, network);
        new CryptoAmount(quotedCryptoAmount, new AssetNetwork(asset, network));
    

        this.orderId = orderId;

        this.customerId = customerId;

        this.asset = asset;

        this.network = network;

        this.quotedCryptoAmount = quotedCryptoAmount;

        this.quoteExpiresAt = quoteExpiresAt;

        this.deposit = deposit;

        this.payout = payout;

        this.counterparty = counterparty;

        this.customerNote = customerNote;

    }

    @Override
    public String screenedAddress() {
        return deposit.fromAddress();
    }

    @Override
    public CryptoAmount requestedCryptoAmount() {
        return new CryptoAmount(quotedCryptoAmount, assetNetworkIdentity());
    }
}
