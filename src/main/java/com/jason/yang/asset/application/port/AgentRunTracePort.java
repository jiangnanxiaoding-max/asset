package com.jason.yang.asset.application.port;

import com.jason.yang.asset.application.model.AgentRunTrace;
import com.jason.yang.asset.domain.OrderId;

import java.util.Optional;

/** Transfers a sanitized Agent trace from investigation to the decision audit boundary. */
public interface AgentRunTracePort {
    void save(OrderId orderId, AgentRunTrace trace);

    Optional<AgentRunTrace> take(OrderId orderId);

    static AgentRunTracePort none() {
        return new AgentRunTracePort() {
            @Override
            public void save(OrderId orderId, AgentRunTrace trace) { }

            @Override
            public Optional<AgentRunTrace> take(OrderId orderId) {
                return Optional.empty();
            }
        };
    }
}
