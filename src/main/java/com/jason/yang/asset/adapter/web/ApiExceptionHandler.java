package com.jason.yang.asset.adapter.web;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Converts adapter failures to a stable, sanitized and machine-readable error taxonomy. */
@RestControllerAdvice
@Profile("!cli")
public final class ApiExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiErrorResponse> unreadable(
            HttpMessageNotReadableException exception, HttpServletRequest request
    ) {
        return error(request, HttpStatus.BAD_REQUEST, "MALFORMED_REQUEST",
                "Request body could not be read", false, java.util.Collections.emptyList());
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiErrorResponse> unexpected(Exception exception, HttpServletRequest request) {
        String requestId = requestId(request);
        log.error("web triage failed requestId={}", requestId, exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ApiErrorResponse(
                Instant.now(), 500, "TRIAGE_PROCESSING_FAILED",
                "The order could not be processed; use requestId when contacting support",
                true, requestId, java.util.Collections.emptyList()));
    }

    private ResponseEntity<ApiErrorResponse> error(
            HttpServletRequest request,
            HttpStatus status,
            String code,
            String message,
            boolean retryable,
            List<com.jason.yang.asset.application.input.InputViolation> violations
    ) {
        String requestId = requestId(request);
        return ResponseEntity.status(status).body(new ApiErrorResponse(
                Instant.now(), status.value(), code, message, retryable, requestId, violations));
    }

    private String requestId(HttpServletRequest request) {
        Object requestId = request.getAttribute(TriageController.REQUEST_ID_ATTRIBUTE);
        return requestId instanceof String ? (String) requestId : UUID.randomUUID().toString();
    }
}
