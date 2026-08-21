package com.jason.yang.asset.infrastructure.agent.llm;

import java.util.concurrent.Semaphore;

/** Thread-safe per-runtime bulkhead, cost budget and simple provider circuit breaker. */
public class AgentBatchBudget {
    private final int maxModelCalls;
    private final long maxTokens;
    private final int circuitFailureThreshold;
    private final Semaphore sessions;
    private int modelCalls;
    private long tokens;
    private int consecutiveFailures;

    public AgentBatchBudget(int maxModelCalls, long maxTokens,
                            int maxConcurrentSessions, int circuitFailureThreshold) {
        if (maxModelCalls <= 0 || maxTokens <= 0 || maxConcurrentSessions <= 0
                || circuitFailureThreshold <= 0) {
            throw new IllegalArgumentException("Agent batch limits must be positive");
        }
        this.maxModelCalls = maxModelCalls;
        this.maxTokens = maxTokens;
        this.circuitFailureThreshold = circuitFailureThreshold;
        this.sessions = new Semaphore(maxConcurrentSessions);
    }

    public static AgentBatchBudget demoDefaults() {
        return new AgentBatchBudget(2_000, 2_000_000L, 4, 5);
    }

    public boolean tryAcquireSession() {
        return sessions.tryAcquire();
    }

    public void releaseSession() {
        sessions.release();
    }

    /** Returns null when reserved, otherwise a stable fail-closed reason code. */
    public synchronized String reserveModelCall() {
        if (consecutiveFailures >= circuitFailureThreshold) return "LLM_CIRCUIT_OPEN";
        if (modelCalls >= maxModelCalls) return "LLM_BATCH_CALL_BUDGET_EXHAUSTED";
        modelCalls++;
        return null;
    }

    public synchronized boolean recordTokens(int usedTokens) {
        if (usedTokens < 0 || tokens + usedTokens > maxTokens) return false;
        tokens += usedTokens;
        return true;
    }

    public synchronized void recordProviderSuccess() {
        consecutiveFailures = 0;
    }

    public synchronized void recordProviderFailure() {
        consecutiveFailures++;
    }
}
