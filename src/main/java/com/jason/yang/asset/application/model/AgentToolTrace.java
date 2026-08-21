package com.jason.yang.asset.application.model;

import java.util.Objects;

/** Sanitized operational trace for one Agent tool invocation. */
public final class AgentToolTrace {
    private final String toolName;
    private final String resultType;
    private final long elapsedMillis;

    public AgentToolTrace(String toolName, String resultType, long elapsedMillis) {
        this.toolName = Objects.requireNonNull(toolName);
        this.resultType = Objects.requireNonNull(resultType);
        this.elapsedMillis = elapsedMillis;
    }

    public String toolName() { return toolName; }
    public String getToolName() { return toolName; }
    public String resultType() { return resultType; }
    public String getResultType() { return resultType; }
    public long elapsedMillis() { return elapsedMillis; }
    public long getElapsedMillis() { return elapsedMillis; }
}
