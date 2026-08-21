package com.jason.yang.asset.domain;

import java.math.BigDecimal;
import java.util.Objects;

public final class WithdrawalOrder implements Order {
    private final String orderId;
    private final String customerId;
    private final String asset;
    private final String network;
    private final BigDecimal amount;
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

    public BigDecimal amount() {
        return amount;
    }

    public BigDecimal getAmount() {
        return amount;
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
        if (!(other instanceof WithdrawalOrder)) return false;
        WithdrawalOrder that = (WithdrawalOrder) other;
        return java.util.Objects.equals(orderId, that.orderId)
                && java.util.Objects.equals(customerId, that.customerId)
                && java.util.Objects.equals(asset, that.asset)
                && java.util.Objects.equals(network, that.network)
                && java.util.Objects.equals(amount, that.amount)
                && java.util.Objects.equals(destinationAddress, that.destinationAddress)
                && java.util.Objects.equals(counterparty, that.counterparty)
                && java.util.Objects.equals(customerNote, that.customerNote);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(orderId, customerId, asset, network, amount, destinationAddress, counterparty, customerNote);
    }

    @Override
    public String toString() {
        return "WithdrawalOrder{" + "orderId=" + orderId + ", customerId=" + customerId + ", asset=" + asset + ", network=" + network + ", amount=" + amount + ", destinationAddress=" + destinationAddress + ", counterparty=" + counterparty + ", customerNote=" + customerNote + "}";
    }


    public WithdrawalOrder(String orderId, String customerId, String asset, String network, BigDecimal amount, String destinationAddress, CounterpartyInfo counterparty, String customerNote) {
        Objects.requireNonNull(orderId);
        Objects.requireNonNull(customerId);
        Objects.requireNonNull(asset);
        Objects.requireNonNull(network);
        Objects.requireNonNull(amount);
        Objects.requireNonNull(destinationAddress);
        counterparty = counterparty == null ? CounterpartyInfo.unknown() : counterparty;
        customerNote = customerNote == null ? "" : customerNote;
        new OrderId(orderId);
        new CustomerId(customerId);
        new AssetNetwork(asset, network);
        new CryptoAmount(amount, new AssetNetwork(asset, network));
    

        this.orderId = orderId;

        this.customerId = customerId;

        this.asset = asset;

        this.network = network;

        this.amount = amount;

        this.destinationAddress = destinationAddress;

        this.counterparty = counterparty;

        this.customerNote = customerNote;

    }

    @Override
    public String screenedAddress() {
        return destinationAddress;
    }

    @Override
    public CryptoAmount requestedCryptoAmount() {
        return new CryptoAmount(amount, assetNetworkIdentity());
    }
}
