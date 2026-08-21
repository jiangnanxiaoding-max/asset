package com.jason.yang.asset.infrastructure.agent.llm;

import java.time.Duration;
import java.util.Objects;

/** Immutable safety budget applied to one LLM investigation session. */
public final class AgentExecutionPolicy {
    private final int maxIterations;
    private final int maxToolCalls;
    private final int maxTokens;
    private final Duration timeout;
    private final int maxRetries;
    private final Duration retryBackoff;

    public AgentExecutionPolicy(
            int maxIterations,
            int maxToolCalls,
            int maxTokens,
            Duration timeout,
            int maxRetries,
            Duration retryBackoff
    ) {
        if (maxIterations <= 0) {
            throw new IllegalArgumentException("maxIterations must be positive");
        }
        if (maxToolCalls <= 0) {
            throw new IllegalArgumentException("maxToolCalls must be positive");
        }
        if (maxTokens <= 0) {
            throw new IllegalArgumentException("maxTokens must be positive");
        }
        if (maxRetries < 0) {
            throw new IllegalArgumentException("maxRetries must not be negative");
        }
        this.timeout = positive(timeout, "timeout");
        this.retryBackoff = nonNegative(retryBackoff, "retryBackoff");
        this.maxIterations = maxIterations;
        this.maxToolCalls = maxToolCalls;
        this.maxTokens = maxTokens;
        this.maxRetries = maxRetries;
    }

    public static AgentExecutionPolicy demoDefaults() {
        return new AgentExecutionPolicy(
                8,
                7,
                8_000,
                Duration.ofSeconds(30),
                2,
                Duration.ofMillis(200)
        );
    }

    public int maxIterations() {
        return maxIterations;
    }

    public int maxToolCalls() {
        return maxToolCalls;
    }

    public int maxTokens() {
        return maxTokens;
    }

    public Duration timeout() {
        return timeout;
    }

    public int maxRetries() {
        return maxRetries;
    }

    public Duration retryBackoff() {
        return retryBackoff;
    }

    private static Duration positive(Duration value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isZero() || value.isNegative()) {
            throw new IllegalArgumentException(name + " must be positive");
        }
        return value;
    }

    private static Duration nonNegative(Duration value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isNegative()) {
            throw new IllegalArgumentException(name + " must not be negative");
        }
        return value;
    }
}
