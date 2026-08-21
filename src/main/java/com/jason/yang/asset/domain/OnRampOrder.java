package com.jason.yang.asset.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

public final class OnRampOrder implements Order {
    private final String orderId;
    private final String customerId;
    private final String asset;
    private final String network;
    private final BigDecimal fiatAmountUsd;
    private final BigDecimal quotedCryptoAmount;
    private final Instant quoteExpiresAt;
    private final FundingEvidence.Status embeddedFiatStatus;
    private final String destinationAddress;
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

    public BigDecimal fiatAmountUsd() {
        return fiatAmountUsd;
    }

    public BigDecimal getFiatAmountUsd() {
        return fiatAmountUsd;
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

    public FundingEvidence.Status embeddedFiatStatus() {
        return embeddedFiatStatus;
    }

    public FundingEvidence.Status getEmbeddedFiatStatus() {
        return embeddedFiatStatus;
    }

    public String destinationAddress() {
        return destinationAddress;
    }

    public String getDestinationAddress() {
        return destinationAddress;
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
        if (!(other instanceof OnRampOrder)) return false;
        OnRampOrder that = (OnRampOrder) other;
        return java.util.Objects.equals(orderId, that.orderId)
                && java.util.Objects.equals(customerId, that.customerId)
                && java.util.Objects.equals(asset, that.asset)
                && java.util.Objects.equals(network, that.network)
                && java.util.Objects.equals(fiatAmountUsd, that.fiatAmountUsd)
                && java.util.Objects.equals(quotedCryptoAmount, that.quotedCryptoAmount)
                && java.util.Objects.equals(quoteExpiresAt, that.quoteExpiresAt)
                && java.util.Objects.equals(embeddedFiatStatus, that.embeddedFiatStatus)
                && java.util.Objects.equals(destinationAddress, that.destinationAddress)
                && java.util.Objects.equals(counterparty, that.counterparty)
                && java.util.Objects.equals(customerNote, that.customerNote);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(orderId, customerId, asset, network, fiatAmountUsd, quotedCryptoAmount, quoteExpiresAt, embeddedFiatStatus, destinationAddress, counterparty, customerNote);
    }

    @Override
    public String toString() {
        return "OnRampOrder{" + "orderId=" + orderId + ", customerId=" + customerId + ", asset=" + asset + ", network=" + network + ", fiatAmountUsd=" + fiatAmountUsd + ", quotedCryptoAmount=" + quotedCryptoAmount + ", quoteExpiresAt=" + quoteExpiresAt + ", embeddedFiatStatus=" + embeddedFiatStatus + ", destinationAddress=" + destinationAddress + ", counterparty=" + counterparty + ", customerNote=" + customerNote + "}";
    }


    public OnRampOrder(String orderId, String customerId, String asset, String network, BigDecimal fiatAmountUsd, BigDecimal quotedCryptoAmount, Instant quoteExpiresAt, FundingEvidence.Status embeddedFiatStatus, String destinationAddress, CounterpartyInfo counterparty, String customerNote) {
        Objects.requireNonNull(orderId);
        Objects.requireNonNull(customerId);
        Objects.requireNonNull(asset);
        Objects.requireNonNull(network);
        Objects.requireNonNull(fiatAmountUsd);
        Objects.requireNonNull(quotedCryptoAmount);
        Objects.requireNonNull(quoteExpiresAt);
        Objects.requireNonNull(embeddedFiatStatus);
        Objects.requireNonNull(destinationAddress);
        counterparty = counterparty == null ? CounterpartyInfo.unknown() : counterparty;
        customerNote = customerNote == null ? "" : customerNote;
        new OrderId(orderId);
        new CustomerId(customerId);
        new AssetNetwork(asset, network);
        new FiatMoney(fiatAmountUsd, "USD");
        new CryptoAmount(quotedCryptoAmount, new AssetNetwork(asset, network));
    

        this.orderId = orderId;

        this.customerId = customerId;

        this.asset = asset;

        this.network = network;

        this.fiatAmountUsd = fiatAmountUsd;

        this.quotedCryptoAmount = quotedCryptoAmount;

        this.quoteExpiresAt = quoteExpiresAt;

        this.embeddedFiatStatus = embeddedFiatStatus;

        this.destinationAddress = destinationAddress;

        this.counterparty = counterparty;

        this.customerNote = customerNote;

    }

    public OnRampOrder(
            String orderId,
            String customerId,
            String asset,
            String network,
            BigDecimal fiatAmountUsd,
            BigDecimal quotedCryptoAmount,
            Instant quoteExpiresAt,
            String destinationAddress,
            CounterpartyInfo counterparty,
            String customerNote
    ) {
        this(orderId, customerId, asset, network, fiatAmountUsd, quotedCryptoAmount,
                quoteExpiresAt, FundingEvidence.Status.CONFIRMED, destinationAddress,
                counterparty, customerNote);
    }

    @Override
    public String screenedAddress() {
        return destinationAddress;
    }

    @Override
    public CryptoAmount requestedCryptoAmount() {
        return new CryptoAmount(quotedCryptoAmount, assetNetworkIdentity());
    }
}
