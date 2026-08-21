package com.jason.yang.asset.application.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** Sanitized Agent evidence attached to the durable decision audit. */
public final class AgentRunTrace {
    private final String provider;
    private final String model;
    private final String promptVersion;
    private final int iterations;
    private final int modelCalls;
    private final int toolCalls;
    private final int retryCount;
    private final int tokensUsed;
    private final long elapsedMillis;
    private final String stopReason;
    private final List<AgentToolTrace> tools;

    public AgentRunTrace(
            String provider,
            String model,
            String promptVersion,
            int iterations,
            int modelCalls,
            int toolCalls,
            int retryCount,
            int tokensUsed,
            long elapsedMillis,
            String stopReason,
            List<AgentToolTrace> tools
    ) {
        this.provider = Objects.requireNonNull(provider);
        this.model = Objects.requireNonNull(model);
        this.promptVersion = Objects.requireNonNull(promptVersion);
        this.iterations = iterations;
        this.modelCalls = modelCalls;
        this.toolCalls = toolCalls;
        this.retryCount = retryCount;
        this.tokensUsed = tokensUsed;
        this.elapsedMillis = elapsedMillis;
        this.stopReason = Objects.requireNonNull(stopReason);
        this.tools = Collections.unmodifiableList(new ArrayList<AgentToolTrace>(tools));
    }

    public String provider() { return provider; }
    public String getProvider() { return provider; }
    public String model() { return model; }
    public String getModel() { return model; }
    public String promptVersion() { return promptVersion; }
    public String getPromptVersion() { return promptVersion; }
    public int iterations() { return iterations; }
    public int getIterations() { return iterations; }
    public int modelCalls() { return modelCalls; }
    public int getModelCalls() { return modelCalls; }
    public int toolCalls() { return toolCalls; }
    public int getToolCalls() { return toolCalls; }
    public int retryCount() { return retryCount; }
    public int getRetryCount() { return retryCount; }
    public int tokensUsed() { return tokensUsed; }
    public int getTokensUsed() { return tokensUsed; }
    public long elapsedMillis() { return elapsedMillis; }
    public long getElapsedMillis() { return elapsedMillis; }
    public String stopReason() { return stopReason; }
    public String getStopReason() { return stopReason; }
    public List<AgentToolTrace> tools() { return tools; }
    public List<AgentToolTrace> getTools() { return tools; }
}
