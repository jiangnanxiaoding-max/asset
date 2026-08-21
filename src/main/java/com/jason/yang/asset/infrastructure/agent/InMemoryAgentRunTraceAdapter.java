package com.jason.yang.asset.infrastructure.agent;

import com.jason.yang.asset.application.model.AgentRunTrace;
import com.jason.yang.asset.application.port.AgentRunTracePort;
import com.jason.yang.asset.domain.OrderId;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/** Per-runtime trace handoff; taking a trace removes it to keep memory bounded. */
public class InMemoryAgentRunTraceAdapter implements AgentRunTracePort {
    private final ConcurrentMap<OrderId, AgentRunTrace> traces =
            new ConcurrentHashMap<OrderId, AgentRunTrace>();

    @Override
    public void save(OrderId orderId, AgentRunTrace trace) {
        traces.put(orderId, trace);
    }

    @Override
    public Optional<AgentRunTrace> take(OrderId orderId) {
        return Optional.ofNullable(traces.remove(orderId));
    }
}
