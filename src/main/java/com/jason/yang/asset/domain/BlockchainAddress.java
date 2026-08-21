package com.jason.yang.asset.domain;

/** Network-qualified blockchain address; format validation belongs to the network adapter. */
public final class BlockchainAddress {
    private final String value;
    private final String network;

    public String value() {
        return value;
    }

    public String getValue() {
        return value;
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
        if (!(other instanceof BlockchainAddress)) return false;
        BlockchainAddress that = (BlockchainAddress) other;
        return java.util.Objects.equals(value, that.value)
                && java.util.Objects.equals(network, that.network);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(value, network);
    }

    @Override
    public String toString() {
        return "BlockchainAddress{" + "value=" + value + ", network=" + network + "}";
    }


    public BlockchainAddress(String value, String network) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("blockchain address must not be blank");
        }
        if (network == null || network.trim().isEmpty()) {
            throw new IllegalArgumentException("address network must not be blank");
        }
    

        this.value = value;

        this.network = network;

    }
}
