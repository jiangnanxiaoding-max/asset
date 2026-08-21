package com.jason.yang.asset.domain;

import java.math.BigDecimal;
import java.util.Objects;

/** Positive crypto quantity whose asset and network are part of its identity. */
public final class CryptoAmount {
    private final BigDecimal value;
    private final AssetNetwork assetNetwork;

    public BigDecimal value() {
        return value;
    }

    public BigDecimal getValue() {
        return value;
    }

    public AssetNetwork assetNetwork() {
        return assetNetwork;
    }

    public AssetNetwork getAssetNetwork() {
        return assetNetwork;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof CryptoAmount)) return false;
        CryptoAmount that = (CryptoAmount) other;
        return java.util.Objects.equals(value, that.value)
                && java.util.Objects.equals(assetNetwork, that.assetNetwork);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(value, assetNetwork);
    }

    @Override
    public String toString() {
        return "CryptoAmount{" + "value=" + value + ", assetNetwork=" + assetNetwork + "}";
    }


    public CryptoAmount(BigDecimal value, AssetNetwork assetNetwork) {
        Objects.requireNonNull(value, "value");
        Objects.requireNonNull(assetNetwork, "assetNetwork");
        if (value.signum() <= 0) {
            throw new IllegalArgumentException("crypto amount must be positive");
        }
    

        this.value = value;

        this.assetNetwork = assetNetwork;

    }

    public boolean sameAssetAndNetwork(CryptoAmount other) {
        return assetNetwork.equals(other.assetNetwork);
    }
}
