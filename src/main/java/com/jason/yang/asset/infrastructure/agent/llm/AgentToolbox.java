package com.jason.yang.asset.infrastructure.agent.llm;

import com.jason.yang.asset.domain.LookupResult;
import com.jason.yang.asset.domain.Order;
import com.jason.yang.asset.domain.PolicySnapshot;
import com.jason.yang.asset.domain.ReferenceRate;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Allow-listed, order-bound tools available to the LLM investigation adapter. */
public interface AgentToolbox {

    Map<String, AgentToolDefinition> tools();

    ToolResult invoke(String toolName, ToolContext context);

    enum FactType {
        CUSTOMER,
        ASSET_POLICY,
        ADDRESS_RISK,
        FUNDING,
        REFERENCE_RATE,
        TRAVEL_RULE,
        DUPLICATE
    }

    final class ToolContext {
        private final Order order;
        private final PolicySnapshot policy;
        private final Optional<LookupResult<ReferenceRate>> referenceRate;

        public ToolContext(
                Order order,
                PolicySnapshot policy,
                Optional<LookupResult<ReferenceRate>> referenceRate
        ) {
            this.order = Objects.requireNonNull(order);
            this.policy = Objects.requireNonNull(policy);
            this.referenceRate = Objects.requireNonNull(referenceRate);
        }

        public Order order() {
            return order;
        }

        public PolicySnapshot policy() {
            return policy;
        }

        public Optional<LookupResult<ReferenceRate>> referenceRate() {
            return referenceRate;
        }
    }

    final class ToolResult {
        private final FactType factType;
        private final LookupResult<?> value;

        public ToolResult(FactType factType, LookupResult<?> value) {
            this.factType = Objects.requireNonNull(factType);
            this.value = Objects.requireNonNull(value);
        }

        public FactType factType() {
            return factType;
        }

        public LookupResult<?> value() {
            return value;
        }
    }
}
