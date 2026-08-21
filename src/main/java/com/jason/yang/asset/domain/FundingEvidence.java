package com.jason.yang.asset.domain;

import java.math.BigDecimal;
import java.util.Objects;

public interface FundingEvidence {
final class Fiat implements FundingEvidence {
    private final Status status;
    private final BigDecimal receivedAmountUsd;

    public Status status() {
        return status;
    }

    public Status getStatus() {
        return status;
    }

    public BigDecimal receivedAmountUsd() {
        return receivedAmountUsd;
    }

    public BigDecimal getReceivedAmountUsd() {
        return receivedAmountUsd;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof Fiat)) return false;
        Fiat that = (Fiat) other;
        return java.util.Objects.equals(status, that.status)
                && java.util.Objects.equals(receivedAmountUsd, that.receivedAmountUsd);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(status, receivedAmountUsd);
    }

    @Override
    public String toString() {
        return "Fiat{" + "status=" + status + ", receivedAmountUsd=" + receivedAmountUsd + "}";
    }


        public Fiat(Status status, BigDecimal receivedAmountUsd) {
            Objects.requireNonNull(status);
            Objects.requireNonNull(receivedAmountUsd);
            if (receivedAmountUsd.signum() < 0) {
                throw new IllegalArgumentException("received fiat amount must not be negative");
            }
        

            this.status = status;

            this.receivedAmountUsd = receivedAmountUsd;

        }
    }static final class Chain implements FundingEvidence {
    private final Status status;
    private final String eventKey;
    private final String observedNetwork;
    private final BigDecimal observedAmount;
    private final int confirmations;

    public Status status() {
        return status;
    }

    public Status getStatus() {
        return status;
    }

    public String eventKey() {
        return eventKey;
    }

    public String getEventKey() {
        return eventKey;
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

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof Chain)) return false;
        Chain that = (Chain) other;
        return java.util.Objects.equals(status, that.status)
                && java.util.Objects.equals(eventKey, that.eventKey)
                && java.util.Objects.equals(observedNetwork, that.observedNetwork)
                && java.util.Objects.equals(observedAmount, that.observedAmount)
                && confirmations == that.confirmations;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(status, eventKey, observedNetwork, observedAmount, confirmations);
    }

    @Override
    public String toString() {
        return "Chain{" + "status=" + status + ", eventKey=" + eventKey + ", observedNetwork=" + observedNetwork + ", observedAmount=" + observedAmount + ", confirmations=" + confirmations + "}";
    }


        public Chain(Status status, String eventKey, String observedNetwork, BigDecimal observedAmount, int confirmations) {
            Objects.requireNonNull(status);
            Objects.requireNonNull(eventKey);
            Objects.requireNonNull(observedNetwork);
            Objects.requireNonNull(observedAmount);
            if (observedAmount.signum() < 0) {
                throw new IllegalArgumentException("observed crypto amount must not be negative");
            }
            if (confirmations < 0) {
                throw new IllegalArgumentException("confirmations must not be negative");
            }
        

            this.status = status;

            this.eventKey = eventKey;

            this.observedNetwork = observedNetwork;

            this.observedAmount = observedAmount;

            this.confirmations = confirmations;

        }
    }static final class Wallet implements FundingEvidence {
    private final Status status;
    private final BigDecimal availableAmount;
    private final BigDecimal reservedAmount;

    public Status status() {
        return status;
    }

    public Status getStatus() {
        return status;
    }

    public BigDecimal availableAmount() {
        return availableAmount;
    }

    public BigDecimal getAvailableAmount() {
        return availableAmount;
    }

    public BigDecimal reservedAmount() {
        return reservedAmount;
    }

    public BigDecimal getReservedAmount() {
        return reservedAmount;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof Wallet)) return false;
        Wallet that = (Wallet) other;
        return java.util.Objects.equals(status, that.status)
                && java.util.Objects.equals(availableAmount, that.availableAmount)
                && java.util.Objects.equals(reservedAmount, that.reservedAmount);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(status, availableAmount, reservedAmount);
    }

    @Override
    public String toString() {
        return "Wallet{" + "status=" + status + ", availableAmount=" + availableAmount + ", reservedAmount=" + reservedAmount + "}";
    }


        public Wallet(Status status, BigDecimal availableAmount, BigDecimal reservedAmount) {
            Objects.requireNonNull(status);
            Objects.requireNonNull(availableAmount);
            Objects.requireNonNull(reservedAmount);
            if (availableAmount.signum() < 0 || reservedAmount.signum() < 0) {
                throw new IllegalArgumentException("wallet amounts must not be negative");
            }
        

            this.status = status;

            this.availableAmount = availableAmount;

            this.reservedAmount = reservedAmount;

        }
    }

    enum Status {
        PENDING,
        CONFIRMED,
        RESERVED,
        REVERSED,
        FAILED,
        UNKNOWN
    }
}
