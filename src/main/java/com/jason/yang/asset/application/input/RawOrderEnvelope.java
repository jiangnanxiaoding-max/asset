package com.jason.yang.asset.application.input;

import java.time.Instant;
import java.util.Objects;

/** Immutable boundary object retaining source identity without exposing raw payload to the domain. */
public final class RawOrderEnvelope {
    private final String sourceName;
    private final long sourcePosition;
    private final String rawPayload;
    private final String payloadSha256;
    private final Instant receivedAt;

    public String sourceName() {
        return sourceName;
    }

    public String getSourceName() {
        return sourceName;
    }

    public long sourcePosition() {
        return sourcePosition;
    }

    public long getSourcePosition() {
        return sourcePosition;
    }

    public String rawPayload() {
        return rawPayload;
    }

    public String getRawPayload() {
        return rawPayload;
    }

    public String payloadSha256() {
        return payloadSha256;
    }

    public String getPayloadSha256() {
        return payloadSha256;
    }

    public Instant receivedAt() {
        return receivedAt;
    }

    public Instant getReceivedAt() {
        return receivedAt;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof RawOrderEnvelope)) return false;
        RawOrderEnvelope that = (RawOrderEnvelope) other;
        return java.util.Objects.equals(sourceName, that.sourceName)
                && sourcePosition == that.sourcePosition
                && java.util.Objects.equals(rawPayload, that.rawPayload)
                && java.util.Objects.equals(payloadSha256, that.payloadSha256)
                && java.util.Objects.equals(receivedAt, that.receivedAt);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(sourceName, sourcePosition, rawPayload, payloadSha256, receivedAt);
    }

    @Override
    public String toString() {
        return "RawOrderEnvelope{" + "sourceName=" + sourceName + ", sourcePosition=" + sourcePosition + ", rawPayload=" + rawPayload + ", payloadSha256=" + payloadSha256 + ", receivedAt=" + receivedAt + "}";
    }


    public RawOrderEnvelope(String sourceName, long sourcePosition, String rawPayload, String payloadSha256, Instant receivedAt) {
        Objects.requireNonNull(sourceName);
        Objects.requireNonNull(rawPayload);
        Objects.requireNonNull(payloadSha256);
        Objects.requireNonNull(receivedAt);
        if (sourcePosition < 1) {
            throw new IllegalArgumentException("sourcePosition must start at one");
        }
    

        this.sourceName = sourceName;

        this.sourcePosition = sourcePosition;

        this.rawPayload = rawPayload;

        this.payloadSha256 = payloadSha256;

        this.receivedAt = receivedAt;

    }
}
