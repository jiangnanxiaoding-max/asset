package com.jason.yang.asset.domain;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Optional;

public final class CustomerProfile {
    private final String customerId;
    private final String legalName;
    private final int kycTier;
    private final BigDecimal monthlyLimitUsd;
    private final Optional<BigDecimal> monthlyUsedUsd;
    private final String verifiedBankName;
    private final Status status;

    public String customerId() {
        return customerId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public String legalName() {
        return legalName;
    }

    public String getLegalName() {
        return legalName;
    }

    public int kycTier() {
        return kycTier;
    }

    public int getKycTier() {
        return kycTier;
    }

    public BigDecimal monthlyLimitUsd() {
        return monthlyLimitUsd;
    }

    public BigDecimal getMonthlyLimitUsd() {
        return monthlyLimitUsd;
    }

    public Optional<BigDecimal> monthlyUsedUsd() {
        return monthlyUsedUsd;
    }

    public Optional<BigDecimal> getMonthlyUsedUsd() {
        return monthlyUsedUsd;
    }

    public String verifiedBankName() {
        return verifiedBankName;
    }

    public String getVerifiedBankName() {
        return verifiedBankName;
    }

    public Status status() {
        return status;
    }

    public Status getStatus() {
        return status;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof CustomerProfile)) return false;
        CustomerProfile that = (CustomerProfile) other;
        return java.util.Objects.equals(customerId, that.customerId)
                && java.util.Objects.equals(legalName, that.legalName)
                && kycTier == that.kycTier
                && java.util.Objects.equals(monthlyLimitUsd, that.monthlyLimitUsd)
                && java.util.Objects.equals(monthlyUsedUsd, that.monthlyUsedUsd)
                && java.util.Objects.equals(verifiedBankName, that.verifiedBankName)
                && java.util.Objects.equals(status, that.status);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(customerId, legalName, kycTier, monthlyLimitUsd, monthlyUsedUsd, verifiedBankName, status);
    }

    @Override
    public String toString() {
        return "CustomerProfile{" + "customerId=" + customerId + ", legalName=" + legalName + ", kycTier=" + kycTier + ", monthlyLimitUsd=" + monthlyLimitUsd + ", monthlyUsedUsd=" + monthlyUsedUsd + ", verifiedBankName=" + verifiedBankName + ", status=" + status + "}";
    }


    public CustomerProfile(String customerId, String legalName, int kycTier, BigDecimal monthlyLimitUsd, Optional<BigDecimal> monthlyUsedUsd, String verifiedBankName, Status status) {
        Objects.requireNonNull(customerId);
        Objects.requireNonNull(legalName);
        Objects.requireNonNull(monthlyLimitUsd);
        monthlyUsedUsd = monthlyUsedUsd == null ? Optional.empty() : monthlyUsedUsd;
        Objects.requireNonNull(verifiedBankName);
        Objects.requireNonNull(status);
        new CustomerId(customerId);
        if (monthlyLimitUsd.signum() <= 0) {
            throw new IllegalArgumentException("monthly limit must be positive");
        }
        monthlyUsedUsd.ifPresent(used -> {
            if (used.signum() < 0) {
                throw new IllegalArgumentException("monthly used amount must not be negative");
            }
        });
    

        this.customerId = customerId;

        this.legalName = legalName;

        this.kycTier = kycTier;

        this.monthlyLimitUsd = monthlyLimitUsd;

        this.monthlyUsedUsd = monthlyUsedUsd;

        this.verifiedBankName = verifiedBankName;

        this.status = status;

    }

    public CustomerId identity() {
        return new CustomerId(customerId);
    }

    public enum Status {
        ACTIVE,
        REVIEW_HOLD,
        SUSPENDED,
        CLOSED,
        UNKNOWN
    }
}
