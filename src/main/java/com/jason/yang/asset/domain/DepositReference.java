package com.jason.yang.asset.domain;

import java.math.BigDecimal;
import java.util.Objects;

public final class DepositReference {
    private final String txHash;
    private final String transferIndex;
    private final String fromAddress;
    private final String observedNetwork;
    private final BigDecimal observedAmount;
    private final int confirmations;
    private final FundingEvidence.Status embeddedStatus;

    public String txHash() {
        return txHash;
    }

    public String getTxHash() {
        return txHash;
    }

    public String transferIndex() {
        return transferIndex;
    }

    public String getTransferIndex() {
        return transferIndex;
    }

    public String fromAddress() {
        return fromAddress;
    }

    public String getFromAddress() {
        return fromAddress;
    }

    public String observedNetwork() {
        return observedNetwork;
    }

    public String getObservedNetwork() {
        return observedNetwork;
    }

    public BigDecimal observedAmount() {
        return observedAmount;
    }

    public BigDecimal getObservedAmount() {
        return observedAmount;
    }

    public int confirmations() {
        return confirmations;
    }

    public int getConfirmations() {
        return confirmations;
    }

    public FundingEvidence.Status embeddedStatus() {
        return embeddedStatus;
    }

    public FundingEvidence.Status getEmbeddedStatus() {
        return embeddedStatus;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof DepositReference)) return false;
        DepositReference that = (DepositReference) other;
        return java.util.Objects.equals(txHash, that.txHash)
                && java.util.Objects.equals(transferIndex, that.transferIndex)
                && java.util.Objects.equals(fromAddress, that.fromAddress)
                && java.util.Objects.equals(observedNetwork, that.observedNetwork)
                && java.util.Objects.equals(observedAmount, that.observedAmount)
                && confirmations == that.confirmations
                && java.util.Objects.equals(embeddedStatus, that.embeddedStatus);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(txHash, transferIndex, fromAddress, observedNetwork, observedAmount, confirmations, embeddedStatus);
    }

    @Override
    public String toString() {
        return "DepositReference{" + "txHash=" + txHash + ", transferIndex=" + transferIndex + ", fromAddress=" + fromAddress + ", observedNetwork=" + observedNetwork + ", observedAmount=" + observedAmount + ", confirmations=" + confirmations + ", embeddedStatus=" + embeddedStatus + "}";
    }


    public DepositReference(String txHash, String transferIndex, String fromAddress, String observedNetwork, BigDecimal observedAmount, int confirmations, FundingEvidence.Status embeddedStatus) {
        Objects.requireNonNull(txHash);
        transferIndex = transferIndex == null ? "0" : transferIndex;
        Objects.requireNonNull(fromAddress);
        Objects.requireNonNull(observedNetwork);
        Objects.requireNonNull(observedAmount);
        Objects.requireNonNull(embeddedStatus);
        if (txHash.trim().isEmpty()) {
            throw new IllegalArgumentException("txHash must not be blank");
        }
        if (fromAddress.trim().isEmpty()) {
            throw new IllegalArgumentException("fromAddress must not be blank");
        }
        new AssetNetwork("REFERENCE", observedNetwork);
        if (observedAmount.signum() <= 0) {
            throw new IllegalArgumentException("observedAmount must be positive");
        }
        if (confirmations < 0) {
            throw new IllegalArgumentException("confirmations must not be negative");
        }
    

        this.txHash = txHash;

        this.transferIndex = transferIndex;

        this.fromAddress = fromAddress;

        this.observedNetwork = observedNetwork;

        this.observedAmount = observedAmount;

        this.confirmations = confirmations;

        this.embeddedStatus = embeddedStatus;

    }

    public DepositReference(
            String txHash,
            String transferIndex,
            String fromAddress,
            String observedNetwork
    ) {
        this(txHash, transferIndex, fromAddress, observedNetwork,
                BigDecimal.ONE, 0, FundingEvidence.Status.CONFIRMED);
    }

    public String eventKey() {
        return observedNetwork + ":" + txHash + ":" + transferIndex;
    }
}
