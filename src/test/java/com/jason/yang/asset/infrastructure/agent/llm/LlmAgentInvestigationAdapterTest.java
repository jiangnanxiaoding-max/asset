package com.jason.yang.asset.infrastructure.agent.llm;

import com.jason.yang.asset.application.model.AgentRunTrace;
import com.jason.yang.asset.infrastructure.agent.InMemoryAgentRunTraceAdapter;
import com.jason.yang.asset.domain.AddressRiskAssessment;
import com.jason.yang.asset.domain.AssetNetworkPolicy;
import com.jason.yang.asset.domain.CounterpartyInfo;
import com.jason.yang.asset.domain.CustomerProfile;
import com.jason.yang.asset.domain.FundingEvidence;
import com.jason.yang.asset.domain.InvestigationFacts;
import com.jason.yang.asset.domain.LookupResult;
import com.jason.yang.asset.domain.Order;
import com.jason.yang.asset.domain.PolicySnapshot;
import com.jason.yang.asset.domain.ReferenceRate;
import com.jason.yang.asset.domain.TravelRuleAssessment;
import com.jason.yang.asset.domain.WithdrawalOrder;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LlmAgentInvestigationAdapterTest {
    private static final Instant NOW = Instant.parse("2026-07-28T12:00:00Z");
    private static final PolicySnapshot POLICY = new PolicySnapshot(
            "policy-test", NOW, new BigDecimal("0.01"), new BigDecimal("1000"));

    @Test
    void enrichesOnlyMissingFactsAndRecordsRetries() {
        ScriptedClient client = new ScriptedClient(2, callsForEveryMissingTool());
        RecordingToolbox toolbox = new RecordingToolbox();
        InMemoryAgentRunTraceAdapter traces = new InMemoryAgentRunTraceAdapter();
        LlmAgentInvestigationAdapter adapter = adapter(client, toolbox, traces, 1_000, 2);

        InvestigationFacts facts = adapter.enrich(order(), POLICY, unresolvedFacts());
        AgentRunTrace trace = traces.take(order().identity()).get();

        assertTrue(facts.customer().isFound());
        assertTrue(facts.travelRule().isFound());
        assertTrue(facts.duplicate() instanceof LookupResult.NotApplicable<?>);
        assertEquals(8, client.calls());
        assertEquals(6, toolbox.invocations());
        assertEquals(2, trace.retryCount());
        assertEquals("FACTS_COMPLETE", trace.stopReason());
    }

    @Test
    void rejectsARepeatedToolThatIsNoLongerEligible() {
        List<LlmAgentClient.Response> responses = Arrays.<LlmAgentClient.Response>asList(
                new LlmAgentClient.ToolCall(PortBackedAgentToolbox.CUSTOMER_PROFILE, 10),
                new LlmAgentClient.ToolCall(PortBackedAgentToolbox.CUSTOMER_PROFILE, 10));
        RecordingToolbox toolbox = new RecordingToolbox();
        InMemoryAgentRunTraceAdapter traces = new InMemoryAgentRunTraceAdapter();
        LlmAgentInvestigationAdapter adapter = adapter(new ScriptedClient(0, responses), toolbox, traces, 1_000, 0);

        InvestigationFacts facts = adapter.enrich(order(), POLICY, unresolvedFacts());

        assertTrue(facts.customer().isFound());
        assertEquals(1, toolbox.invocations());
        assertEquals("LLM_TOOL_NOT_ALLOWED_OR_NOT_READY",
                traces.take(order().identity()).get().stopReason());
    }

    @Test
    void tokenLimitStopsBeforeExecutingTool() {
        RecordingToolbox toolbox = new RecordingToolbox();
        InMemoryAgentRunTraceAdapter traces = new InMemoryAgentRunTraceAdapter();
        LlmAgentInvestigationAdapter adapter = adapter(
                new ScriptedClient(0, Collections.<LlmAgentClient.Response>singletonList(
                        new LlmAgentClient.ToolCall(PortBackedAgentToolbox.CUSTOMER_PROFILE, 101))),
                toolbox, traces, 100, 0);

        InvestigationFacts facts = adapter.enrich(order(), POLICY, unresolvedFacts());

        assertTrue(facts.customer() instanceof LookupResult.Unavailable<?>);
        assertEquals(0, toolbox.invocations());
        assertEquals("LLM_MAX_TOKENS_EXCEEDED", traces.take(order().identity()).get().stopReason());
    }

    @Test
    void modelCannotFinishWhileFactsAreMissing() {
        InMemoryAgentRunTraceAdapter traces = new InMemoryAgentRunTraceAdapter();
        LlmAgentInvestigationAdapter adapter = adapter(
                new ScriptedClient(0, Collections.<LlmAgentClient.Response>singletonList(
                        new LlmAgentClient.Finish(10))), new RecordingToolbox(), traces, 1_000, 0);

        adapter.enrich(order(), POLICY, unresolvedFacts());

        assertEquals("LLM_INCOMPLETE_FACTS", traces.take(order().identity()).get().stopReason());
    }

    private LlmAgentInvestigationAdapter adapter(ScriptedClient client, RecordingToolbox toolbox,
                                                  InMemoryAgentRunTraceAdapter traces,
                                                  int maxTokens, int retries) {
        AgentExecutionPolicy policy = new AgentExecutionPolicy(8, 7, maxTokens,
                Duration.ofSeconds(2), retries, Duration.ZERO);
        return new LlmAgentInvestigationAdapter(client, toolbox, policy,
                new AgentBatchBudget(100, 100_000, 2, 5), traces);
    }

    private List<LlmAgentClient.Response> callsForEveryMissingTool() {
        List<LlmAgentClient.Response> responses = new ArrayList<LlmAgentClient.Response>();
        responses.add(new LlmAgentClient.ToolCall(PortBackedAgentToolbox.CUSTOMER_PROFILE, 10));
        responses.add(new LlmAgentClient.ToolCall(PortBackedAgentToolbox.ASSET_POLICY, 10));
        responses.add(new LlmAgentClient.ToolCall(PortBackedAgentToolbox.ADDRESS_RISK, 10));
        responses.add(new LlmAgentClient.ToolCall(PortBackedAgentToolbox.FUNDING, 10));
        responses.add(new LlmAgentClient.ToolCall(PortBackedAgentToolbox.REFERENCE_RATE, 10));
        responses.add(new LlmAgentClient.ToolCall(PortBackedAgentToolbox.TRAVEL_RULE, 10));
        return responses;
    }

    private InvestigationFacts unresolvedFacts() {
        return new InvestigationFacts(order(), unavailable(), unavailable(), unavailable(), unavailable(),
                unavailable(), unavailable(), LookupResult.notApplicable());
    }

    private <T> LookupResult<T> unavailable() { return LookupResult.unavailable("UPSTREAM_UNAVAILABLE", true); }

    private Order order() {
        return new WithdrawalOrder("O-LLM-DEMO", "c001", "BTC", "BTC", new BigDecimal("0.1"),
                "0xCLEAN01", CounterpartyInfo.directCustomer(), "");
    }

    private static final class ScriptedClient implements LlmAgentClient {
        private int failuresRemaining;
        private int calls;
        private final Deque<Response> responses;
        private ScriptedClient(int failuresRemaining, List<Response> responses) {
            this.failuresRemaining = failuresRemaining;
            this.responses = new ArrayDeque<Response>(responses);
        }
        @Override public Response next(Request request, Duration timeout) {
            calls++;
            if (failuresRemaining > 0) {
                failuresRemaining--;
                throw new ClientException("temporary", true);
            }
            return responses.removeFirst();
        }
        @Override public String provider() { return "test"; }
        @Override public String model() { return "scripted"; }
        private int calls() { return calls; }
    }

    private static final class RecordingToolbox implements AgentToolbox {
        private final Map<String, AgentToolDefinition> tools;
        private int invocations;
        private RecordingToolbox() {
            Map<String, AgentToolDefinition> configured = new LinkedHashMap<String, AgentToolDefinition>();
            add(configured, PortBackedAgentToolbox.CUSTOMER_PROFILE, FactType.CUSTOMER,
                    Collections.<FactType>emptyList());
            add(configured, PortBackedAgentToolbox.ASSET_POLICY, FactType.ASSET_POLICY,
                    Collections.<FactType>emptyList());
            add(configured, PortBackedAgentToolbox.ADDRESS_RISK, FactType.ADDRESS_RISK,
                    Collections.<FactType>emptyList());
            add(configured, PortBackedAgentToolbox.FUNDING, FactType.FUNDING,
                    Collections.<FactType>emptyList());
            add(configured, PortBackedAgentToolbox.REFERENCE_RATE, FactType.REFERENCE_RATE,
                    Collections.<FactType>emptyList());
            add(configured, PortBackedAgentToolbox.TRAVEL_RULE, FactType.TRAVEL_RULE,
                    Collections.singletonList(FactType.REFERENCE_RATE));
            tools = Collections.unmodifiableMap(configured);
        }
        private void add(Map<String, AgentToolDefinition> target, String name, FactType type,
                         List<FactType> prerequisites) {
            target.put(name, new AgentToolDefinition(name, "test " + name, type,
                    Collections.singletonList("WithdrawalOrder"), prerequisites));
        }
        @Override public Map<String, AgentToolDefinition> tools() { return tools; }
        @Override public ToolResult invoke(String name, ToolContext context) {
            invocations++;
            if (PortBackedAgentToolbox.CUSTOMER_PROFILE.equals(name))
                return result(FactType.CUSTOMER, LookupResult.found(new CustomerProfile("c001", "Alice", 2,
                        new BigDecimal("100000"), Optional.<BigDecimal>empty(), "Alice",
                        CustomerProfile.Status.ACTIVE), "customer:c001"));
            if (PortBackedAgentToolbox.ASSET_POLICY.equals(name))
                return result(FactType.ASSET_POLICY, LookupResult.found(new AssetNetworkPolicy("BTC", "BTC",
                        new BigDecimal("0.0005"), 3, 8, RoundingMode.DOWN), "asset:BTC:BTC"));
            if (PortBackedAgentToolbox.ADDRESS_RISK.equals(name))
                return result(FactType.ADDRESS_RISK, LookupResult.found(new AddressRiskAssessment(5,
                        AddressRiskAssessment.RiskCategory.CLEAN, NOW, "risk:clean"), "risk:clean"));
            if (PortBackedAgentToolbox.FUNDING.equals(name))
                return result(FactType.FUNDING, LookupResult.found(new FundingEvidence.Wallet(
                        FundingEvidence.Status.RESERVED, BigDecimal.ONE, new BigDecimal("0.1")), "wallet"));
            if (PortBackedAgentToolbox.REFERENCE_RATE.equals(name))
                return result(FactType.REFERENCE_RATE, LookupResult.found(new ReferenceRate("BTC", "USD",
                        new BigDecimal("67000"), NOW, "rates"), "rate"));
            if (PortBackedAgentToolbox.TRAVEL_RULE.equals(name))
                return result(FactType.TRAVEL_RULE,
                        LookupResult.found(TravelRuleAssessment.notRequired(), "travel"));
            throw new IllegalArgumentException(name);
        }
        private ToolResult result(FactType type, LookupResult<?> value) { return new ToolResult(type, value); }
        private int invocations() { return invocations; }
    }
}
