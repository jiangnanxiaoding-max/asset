package com.jason.yang.asset.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public final class AssetNetworkPolicy {
    private final String asset;
    private final String network;
    private final BigDecimal minimumAmount;
    private final int confirmationsRequired;
    private final int amountScale;
    private final RoundingMode roundingMode;

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

    public BigDecimal minimumAmount() {
        return minimumAmount;
    }

    public BigDecimal getMinimumAmount() {
        return minimumAmount;
    }

    public int confirmationsRequired() {
        return confirmationsRequired;
    }

    public int getConfirmationsRequired() {
        return confirmationsRequired;
    }

    public int amountScale() {
        return amountScale;
    }

    public int getAmountScale() {
        return amountScale;
    }

    public RoundingMode roundingMode() {
        return roundingMode;
    }

    public RoundingMode getRoundingMode() {
        return roundingMode;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof AssetNetworkPolicy)) return false;
        AssetNetworkPolicy that = (AssetNetworkPolicy) other;
        return java.util.Objects.equals(asset, that.asset)
                && java.util.Objects.equals(network, that.network)
                && java.util.Objects.equals(minimumAmount, that.minimumAmount)
                && confirmationsRequired == that.confirmationsRequired
                && amountScale == that.amountScale
                && java.util.Objects.equals(roundingMode, that.roundingMode);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(asset, network, minimumAmount, confirmationsRequired, amountScale, roundingMode);
    }

    @Override
    public String toString() {
        return "AssetNetworkPolicy{" + "asset=" + asset + ", network=" + network + ", minimumAmount=" + minimumAmount + ", confirmationsRequired=" + confirmationsRequired + ", amountScale=" + amountScale + ", roundingMode=" + roundingMode + "}";
    }


    public AssetNetworkPolicy(String asset, String network, BigDecimal minimumAmount, int confirmationsRequired, int amountScale, RoundingMode roundingMode) {
        Objects.requireNonNull(asset);
        Objects.requireNonNull(network);
        Objects.requireNonNull(minimumAmount);
        Objects.requireNonNull(roundingMode);
        new AssetNetwork(asset, network);
        if (minimumAmount.signum() <= 0) {
            throw new IllegalArgumentException("minimum amount must be positive");
        }
        if (confirmationsRequired < 0) {
            throw new IllegalArgumentException("confirmations required must not be negative");
        }
        if (amountScale < 0) {
            throw new IllegalArgumentException("amount scale must not be negative");
        }
    

        this.asset = asset;

        this.network = network;

        this.minimumAmount = minimumAmount;

        this.confirmationsRequired = confirmationsRequired;

        this.amountScale = amountScale;

        this.roundingMode = roundingMode;

    }

    public AssetNetwork assetNetwork() {
        return new AssetNetwork(asset, network);
    }
}
