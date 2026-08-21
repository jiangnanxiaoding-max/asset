package com.jason.yang.asset.infrastructure.agent.llm;

import java.time.Duration;

/** Offline deterministic model stub that selects the first currently eligible tool. */
public class StubLlmAgentClient implements LlmAgentClient {
    @Override
    public Response next(Request request, Duration timeout) {
        if (request.availableTools().isEmpty()) return new Finish(0);
        return new ToolCall(request.availableTools().get(0).name(), 0);
    }

    @Override
    public String provider() { return "stub"; }

    @Override
    public String model() { return "offline-stub-v1"; }
}
