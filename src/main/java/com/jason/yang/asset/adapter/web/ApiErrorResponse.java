package com.jason.yang.asset.adapter.web;

import com.jason.yang.asset.application.input.InputViolation;

import java.time.Instant;
import java.util.List;

/** Machine-readable API error contract shared by all Web adapter failures. */
public final class ApiErrorResponse {
    private final Instant timestamp;
    private final int status;
    private final String code;
    private final String message;
    private final boolean retryable;
    private final String requestId;
    private final List<InputViolation> violations;

    public ApiErrorResponse(Instant timestamp, int status, String code, String message, boolean retryable, String requestId, List<InputViolation> violations) {

        this.timestamp = timestamp;
        this.status = status;
        this.code = code;
        this.message = message;
        this.retryable = retryable;
        this.requestId = requestId;
        this.violations = violations;
    }

    public Instant timestamp() {
        return timestamp;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public int status() {
        return status;
    }

    public int getStatus() {
        return status;
    }

    public String code() {
        return code;
    }

    public String getCode() {
        return code;
    }

    public String message() {
        return message;
    }

    public String getMessage() {
        return message;
    }

    public boolean retryable() {
        return retryable;
    }

    public boolean getRetryable() {
        return retryable;
    }

    public String requestId() {
        return requestId;
    }

    public String getRequestId() {
        return requestId;
    }

    public List<InputViolation> violations() {
        return violations;
    }

    public List<InputViolation> getViolations() {
        return violations;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof ApiErrorResponse)) return false;
        ApiErrorResponse that = (ApiErrorResponse) other;
        return java.util.Objects.equals(timestamp, that.timestamp)
                && status == that.status
                && java.util.Objects.equals(code, that.code)
                && java.util.Objects.equals(message, that.message)
                && retryable == that.retryable
                && java.util.Objects.equals(requestId, that.requestId)
                && java.util.Objects.equals(violations, that.violations);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(timestamp, status, code, message, retryable, requestId, violations);
    }

    @Override
    public String toString() {
        return "ApiErrorResponse{" + "timestamp=" + timestamp + ", status=" + status + ", code=" + code + ", message=" + message + ", retryable=" + retryable + ", requestId=" + requestId + ", violations=" + violations + "}";
    }


}
