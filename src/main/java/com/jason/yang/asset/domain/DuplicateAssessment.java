package com.jason.yang.asset.domain;

public final class DuplicateAssessment {
    private final Status status;
    private final String originalOrderId;

    public Status status() {
        return status;
    }

    public Status getStatus() {
        return status;
    }

    public String originalOrderId() {
        return originalOrderId;
    }

    public String getOriginalOrderId() {
        return originalOrderId;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof DuplicateAssessment)) return false;
        DuplicateAssessment that = (DuplicateAssessment) other;
        return java.util.Objects.equals(status, that.status)
                && java.util.Objects.equals(originalOrderId, that.originalOrderId);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(status, originalOrderId);
    }

    @Override
    public String toString() {
        return "DuplicateAssessment{" + "status=" + status + ", originalOrderId=" + originalOrderId + "}";
    }


    public DuplicateAssessment(Status status, String originalOrderId) {
        originalOrderId = originalOrderId == null ? "" : originalOrderId;
    

        this.status = status;

        this.originalOrderId = originalOrderId;

    }

    public enum Status {
        NEW,
        ALREADY_CREDITED,
        IN_PROGRESS,
        CONFLICT
    }
}
