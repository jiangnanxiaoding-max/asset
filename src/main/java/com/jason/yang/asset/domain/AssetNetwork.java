package com.jason.yang.asset.domain;

/** A supported asset is always interpreted together with its settlement network. */
public final class AssetNetwork {
    private final String asset;
    private final String network;

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

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof AssetNetwork)) return false;
        AssetNetwork that = (AssetNetwork) other;
        return java.util.Objects.equals(asset, that.asset)
                && java.util.Objects.equals(network, that.network);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(asset, network);
    }

    @Override
    public String toString() {
        return "AssetNetwork{" + "asset=" + asset + ", network=" + network + "}";
    }


    public AssetNetwork(String asset, String network) {
        if (asset == null || asset.trim().isEmpty()) {
            throw new IllegalArgumentException("asset must not be blank");
        }
        if (network == null || network.trim().isEmpty()) {
            throw new IllegalArgumentException("network must not be blank");
        }
    

        this.asset = asset;

        this.network = network;

    }
}
