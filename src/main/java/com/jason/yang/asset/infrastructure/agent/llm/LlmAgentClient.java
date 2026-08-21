package com.jason.yang.asset.infrastructure.agent.llm;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** Vendor-neutral boundary for one structured LLM turn. Implementations must honor the supplied timeout. */
public interface LlmAgentClient {

    Response next(Request request, Duration timeout);

    default String provider() { return "unknown"; }

    default String model() { return "unknown"; }

    /** Sanitized prompt data. Raw order JSON, customer notes and tool results are intentionally excluded. */
    final class Request {
        private final String orderId;
        private final String orderType;
        private final String asset;
        private final String network;
        private final String policyVersion;
        private final int iteration;
        private final int remainingToolCalls;
        private final int remainingTokens;
        private final List<AgentToolDefinition> availableTools;
        private final List<String> missingFacts;
        private final List<String> completedTools;
        private final List<String> observationSummaries;

        public Request(
                String orderId,
                String orderType,
                String asset,
                String network,
                String policyVersion,
                int iteration,
                int remainingToolCalls,
                int remainingTokens,
                List<AgentToolDefinition> availableTools,
                List<String> missingFacts,
                List<String> completedTools,
                List<String> observationSummaries
        ) {
            this.orderId = Objects.requireNonNull(orderId);
            this.orderType = Objects.requireNonNull(orderType);
            this.asset = Objects.requireNonNull(asset);
            this.network = Objects.requireNonNull(network);
            this.policyVersion = Objects.requireNonNull(policyVersion);
            this.iteration = iteration;
            this.remainingToolCalls = remainingToolCalls;
            this.remainingTokens = remainingTokens;
            this.availableTools = Collections.unmodifiableList(
                    new ArrayList<AgentToolDefinition>(availableTools));
            this.missingFacts = immutableCopy(missingFacts);
            this.completedTools = immutableCopy(completedTools);
            this.observationSummaries = immutableCopy(observationSummaries);
        }

        public String orderId() {
            return orderId;
        }

        public String orderType() {
            return orderType;
        }

        public String asset() {
            return asset;
        }

        public String network() {
            return network;
        }

        public String policyVersion() {
            return policyVersion;
        }

        public int iteration() {
            return iteration;
        }

        public int remainingToolCalls() {
            return remainingToolCalls;
        }

        public int remainingTokens() {
            return remainingTokens;
        }

        public List<AgentToolDefinition> availableTools() {
            return availableTools;
        }

        public List<String> missingFacts() {
            return missingFacts;
        }

        public List<String> completedTools() {
            return completedTools;
        }

        public List<String> observationSummaries() {
            return observationSummaries;
        }

        private static List<String> immutableCopy(List<String> values) {
            Objects.requireNonNull(values);
            return Collections.unmodifiableList(new ArrayList<String>(values));
        }
    }

    interface Response {
        int tokensUsed();
    }

    /** Structured tool request. Tool arguments are not accepted; every tool is bound to the current order. */
    final class ToolCall implements Response {
        private final String toolName;
        private final int tokensUsed;

        public ToolCall(String toolName, int tokensUsed) {
            this.toolName = Objects.requireNonNull(toolName);
            this.tokensUsed = tokensUsed;
        }

        public String toolName() {
            return toolName;
        }

        @Override
        public int tokensUsed() {
            return tokensUsed;
        }
    }

    /** Allows the model to stop. The adapter accepts it only after every required fact slot is populated. */
    final class Finish implements Response {
        private final int tokensUsed;

        public Finish(int tokensUsed) {
            this.tokensUsed = tokensUsed;
        }

        @Override
        public int tokensUsed() {
            return tokensUsed;
        }
    }

    /** Signals whether a failed model call is safe to retry. */
    class ClientException extends RuntimeException {
        private final boolean retryable;

        public ClientException(String message, boolean retryable) {
            super(message);
            this.retryable = retryable;
        }

        public ClientException(String message, boolean retryable, Throwable cause) {
            super(message, cause);
            this.retryable = retryable;
        }

        public boolean retryable() {
            return retryable;
        }
    }
}
