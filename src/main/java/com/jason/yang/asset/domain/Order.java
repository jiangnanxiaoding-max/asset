package com.jason.yang.asset.domain;

public interface Order {
    String orderId();

    String customerId();

    String asset();

    String network();

    CounterpartyInfo counterparty();

    String customerNote();

    String screenedAddress();

    default OrderId identity() {
        return new OrderId(orderId());
    }

    default CustomerId customerIdentity() {
        return new CustomerId(customerId());
    }

    default AssetNetwork assetNetworkIdentity() {
        return new AssetNetwork(asset(), network());
    }

    CryptoAmount requestedCryptoAmount();
}
