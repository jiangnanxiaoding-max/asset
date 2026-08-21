package com.jason.yang.asset.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

public final class ReferenceRate {
    private final String asset;
    private final String quoteCurrency;
    private final BigDecimal rate;
    private final Instant observedAt;
    private final String source;

    public String asset() {
        return asset;
    }

    public String getAsset() {
        return asset;
    }

    public String quoteCurrency() {
        return quoteCurrency;
    }

    public String getQuoteCurrency() {
        return quoteCurrency;
    }

    public BigDecimal rate() {
        return rate;
    }

    public BigDecimal getRate() {
        return rate;
    }

    public Instant observedAt() {
        return observedAt;
    }

    public Instant getObservedAt() {
        return observedAt;
    }

    public String source() {
        return source;
    }

    public String getSource() {
        return source;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof ReferenceRate)) return false;
        ReferenceRate that = (ReferenceRate) other;
        return java.util.Objects.equals(asset, that.asset)
                && java.util.Objects.equals(quoteCurrency, that.quoteCurrency)
                && java.util.Objects.equals(rate, that.rate)
                && java.util.Objects.equals(observedAt, that.observedAt)
                && java.util.Objects.equals(source, that.source);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(asset, quoteCurrency, rate, observedAt, source);
    }

    @Override
    public String toString() {
        return "ReferenceRate{" + "asset=" + asset + ", quoteCurrency=" + quoteCurrency + ", rate=" + rate + ", observedAt=" + observedAt + ", source=" + source + "}";
    }


    public ReferenceRate(String asset, String quoteCurrency, BigDecimal rate, Instant observedAt, String source) {
        Objects.requireNonNull(asset);
        Objects.requireNonNull(quoteCurrency);
        Objects.requireNonNull(rate);
        Objects.requireNonNull(observedAt);
        Objects.requireNonNull(source);
        if (rate.signum() <= 0) {
            throw new IllegalArgumentException("reference rate must be positive");
        }
    

        this.asset = asset;

        this.quoteCurrency = quoteCurrency;

        this.rate = rate;

        this.observedAt = observedAt;

        this.source = source;

    }
}
