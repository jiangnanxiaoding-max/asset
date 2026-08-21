package com.jason.yang.asset.infrastructure.agent.llm;

import com.jason.yang.asset.application.model.AgentRunTrace;
import com.jason.yang.asset.application.model.AgentToolTrace;
import com.jason.yang.asset.application.port.AgentEnrichmentPort;
import com.jason.yang.asset.application.port.AgentRunTracePort;
import com.jason.yang.asset.domain.InvestigationFacts;
import com.jason.yang.asset.domain.LookupResult;
import com.jason.yang.asset.domain.Order;
import com.jason.yang.asset.domain.PolicySnapshot;
import com.jason.yang.asset.domain.ReferenceRate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/** Bounded LLM enrichment. The model chooses queries; deterministic domain policy makes the decision. */
public class LlmAgentInvestigationAdapter implements AgentEnrichmentPort {
    private static final Logger log = LoggerFactory.getLogger(LlmAgentInvestigationAdapter.class);
    private static final String PROMPT_VERSION = "triage-tools-v1";

    private final LlmAgentClient client;
    private final AgentToolbox toolbox;
    private final AgentExecutionPolicy executionPolicy;
    private final AgentBatchBudget batchBudget;
    private final AgentRunTracePort tracePort;

    public LlmAgentInvestigationAdapter(LlmAgentClient client, AgentToolbox toolbox,
                                        AgentExecutionPolicy executionPolicy) {
        this(client, toolbox, executionPolicy, AgentBatchBudget.demoDefaults(), AgentRunTracePort.none());
    }

    public LlmAgentInvestigationAdapter(LlmAgentClient client, AgentToolbox toolbox,
                                        AgentExecutionPolicy executionPolicy, AgentBatchBudget batchBudget,
                                        AgentRunTracePort tracePort) {
        this.client = Objects.requireNonNull(client);
        this.toolbox = Objects.requireNonNull(toolbox);
        this.executionPolicy = Objects.requireNonNull(executionPolicy);
        this.batchBudget = Objects.requireNonNull(batchBudget);
        this.tracePort = Objects.requireNonNull(tracePort);
        if (toolbox.tools().isEmpty()) throw new IllegalArgumentException("At least one allow-listed tool is required");
    }

    @Override
    public InvestigationFacts enrich(Order order, PolicySnapshot policy, InvestigationFacts currentFacts) {
        Objects.requireNonNull(order, "order");
        Objects.requireNonNull(policy, "policy");
        Objects.requireNonNull(currentFacts, "currentFacts");
        long started = System.nanoTime();
        Counters counters = new Counters();
        FactAccumulator facts = new FactAccumulator(currentFacts);
        Set<String> completed = new LinkedHashSet<String>();
        List<String> observations = new ArrayList<String>();
        List<AgentToolTrace> toolTraces = new ArrayList<AgentToolTrace>();
        boolean acquired = batchBudget.tryAcquireSession();
        if (!acquired) return stopped(order, facts, counters, toolTraces, started, "LLM_CONCURRENCY_LIMIT");

        try {
            for (int iteration = 1; iteration <= executionPolicy.maxIterations(); iteration++) {
                counters.iterations = iteration;
                ensureTimeAvailable(started);
                InvestigationPlan plan = plan(order, facts, completed);
                if (plan.complete()) return stopped(order, facts, counters, toolTraces, started, "FACTS_COMPLETE");
                if (plan.eligibleTools().isEmpty()) {
                    return stopped(order, facts, counters, toolTraces, started, "LLM_NO_ELIGIBLE_TOOLS");
                }

                LlmAgentClient.Request request = new LlmAgentClient.Request(
                        order.orderId(), order.getClass().getSimpleName(), order.asset(), order.network(),
                        policy.version(), iteration, executionPolicy.maxToolCalls() - counters.toolCalls,
                        executionPolicy.maxTokens() - counters.tokens, plan.eligibleTools(),
                        factNames(plan.missingFacts()), new ArrayList<String>(completed), observations);
                LlmAgentClient.Response response = callModel(request, started, counters);
                validateTokens(response, counters.tokens);
                if (!batchBudget.recordTokens(response.tokensUsed())) throw new AgentStopped("LLM_BATCH_TOKEN_BUDGET_EXHAUSTED");
                counters.tokens += response.tokensUsed();

                if (response instanceof LlmAgentClient.Finish) throw new AgentStopped("LLM_INCOMPLETE_FACTS");
                if (!(response instanceof LlmAgentClient.ToolCall)) throw new AgentStopped("LLM_INVALID_RESPONSE");
                LlmAgentClient.ToolCall call = (LlmAgentClient.ToolCall) response;
                AgentToolDefinition definition = eligible(plan, call.toolName());
                if (definition == null) throw new AgentStopped("LLM_TOOL_NOT_ALLOWED_OR_NOT_READY");
                if (completed.contains(call.toolName())) throw new AgentStopped("LLM_DUPLICATE_TOOL_CALL");
                if (counters.toolCalls >= executionPolicy.maxToolCalls()) throw new AgentStopped("LLM_MAX_TOOL_CALLS_EXCEEDED");

                long toolStarted = System.nanoTime();
                AgentToolbox.ToolResult result = toolbox.invoke(call.toolName(),
                        new AgentToolbox.ToolContext(order, policy, facts.referenceRate()));
                if (result == null || result.factType() != definition.producesFact()) {
                    throw new AgentStopped("LLM_INVALID_TOOL_RESULT");
                }
                facts.put(result.factType(), result.value());
                completed.add(call.toolName());
                counters.toolCalls++;
                String resultType = resultType(result.value());
                observations.add(call.toolName() + "=" + resultType);
                toolTraces.add(new AgentToolTrace(call.toolName(), resultType, elapsedMillis(toolStarted)));
            }
            throw new AgentStopped("LLM_MAX_ITERATIONS_EXCEEDED");
        } catch (AgentStopped stopped) {
            return stopped(order, facts, counters, toolTraces, started, stopped.code());
        } catch (RuntimeException exception) {
            log.error("LLM enrichment failed closed orderId={}", order.orderId(), exception);
            return stopped(order, facts, counters, toolTraces, started, "LLM_AGENT_FAILURE");
        } finally {
            batchBudget.releaseSession();
        }
    }

    private InvestigationPlan plan(Order order, FactAccumulator facts, Set<String> completed) {
        List<AgentToolbox.FactType> missing = facts.missing();
        List<AgentToolDefinition> eligible = new ArrayList<AgentToolDefinition>();
        for (AgentToolDefinition tool : toolbox.tools().values()) {
            if (!completed.contains(tool.name()) && tool.appliesTo(order)
                    && missing.contains(tool.producesFact()) && facts.resolved(tool.prerequisites())) {
                eligible.add(tool);
            }
        }
        return new InvestigationPlan(missing, eligible);
    }

    private LlmAgentClient.Response callModel(LlmAgentClient.Request request, long started, Counters counters) {
        int attempt = 0;
        while (true) {
            String budgetStop = batchBudget.reserveModelCall();
            if (budgetStop != null) throw new AgentStopped(budgetStop);
            counters.modelCalls++;
            try {
                LlmAgentClient.Response response = client.next(request, remainingTime(started));
                if (response == null) throw new AgentStopped("LLM_EMPTY_RESPONSE");
                batchBudget.recordProviderSuccess();
                return response;
            } catch (LlmAgentClient.ClientException exception) {
                batchBudget.recordProviderFailure();
                if (!exception.retryable()) throw new AgentStopped("LLM_NON_RETRYABLE_FAILURE");
                if (attempt >= executionPolicy.maxRetries()) throw new AgentStopped("LLM_RETRIES_EXHAUSTED");
                attempt++;
                counters.retries++;
                backoff(started, attempt);
            }
        }
    }

    private InvestigationFacts stopped(Order order, FactAccumulator facts, Counters counters,
                                       List<AgentToolTrace> tools, long started, String reason) {
        tracePort.save(order.identity(), new AgentRunTrace(client.provider(), client.model(), PROMPT_VERSION,
                counters.iterations, counters.modelCalls, counters.toolCalls, counters.retries,
                counters.tokens, elapsedMillis(started), reason, tools));
        log.info("LLM enrichment stopped orderId={} reason={} calls={} tools={} tokens={}",
                order.orderId(), reason, counters.modelCalls, counters.toolCalls, counters.tokens);
        return facts.toFacts(order);
    }

    private AgentToolDefinition eligible(InvestigationPlan plan, String name) {
        for (AgentToolDefinition tool : plan.eligibleTools()) if (tool.name().equals(name)) return tool;
        return null;
    }

    private List<String> factNames(List<AgentToolbox.FactType> types) {
        List<String> names = new ArrayList<String>();
        for (AgentToolbox.FactType type : types) names.add(type.name());
        return names;
    }

    private void validateTokens(LlmAgentClient.Response response, int used) {
        if (response.tokensUsed() < 0) throw new AgentStopped("LLM_INVALID_TOKEN_USAGE");
        if (response.tokensUsed() > executionPolicy.maxTokens() - used) throw new AgentStopped("LLM_MAX_TOKENS_EXCEEDED");
    }

    private void backoff(long started, int attempt) {
        long base = executionPolicy.retryBackoff().toMillis();
        long exponential = Math.min(base * (1L << Math.min(attempt - 1, 8)), 5_000L);
        long millis = exponential == 0 ? 0 : ThreadLocalRandom.current().nextLong(exponential + 1);
        if (millis >= remainingTime(started).toMillis()) throw new AgentStopped("LLM_TIMEOUT");
        try { Thread.sleep(millis); }
        catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AgentStopped("LLM_AGENT_INTERRUPTED");
        }
    }

    private Duration remainingTime(long started) {
        long remaining = executionPolicy.timeout().toNanos() - (System.nanoTime() - started);
        if (remaining <= 0) throw new AgentStopped("LLM_TIMEOUT");
        return Duration.ofNanos(remaining);
    }

    private void ensureTimeAvailable(long started) { remainingTime(started); }
    private long elapsedMillis(long started) { return (System.nanoTime() - started) / 1_000_000L; }

    private String resultType(LookupResult<?> result) {
        if (result instanceof LookupResult.Found<?>) return "FOUND";
        if (result instanceof LookupResult.NotFound<?>) return "NOT_FOUND";
        if (result instanceof LookupResult.Unavailable<?>) return "UNAVAILABLE";
        if (result instanceof LookupResult.Conflict<?>) return "CONFLICT";
        return "NOT_APPLICABLE";
    }

    private static final class Counters {
        private int iterations;
        private int modelCalls;
        private int toolCalls;
        private int retries;
        private int tokens;
    }

    private static final class AgentStopped extends RuntimeException {
        private final String code;
        private AgentStopped(String code) { super(code, null, false, false); this.code = code; }
        private String code() { return code; }
    }

    private static final class FactAccumulator {
        private final Map<AgentToolbox.FactType, LookupResult<?>> values =
                new EnumMap<AgentToolbox.FactType, LookupResult<?>>(AgentToolbox.FactType.class);

        private FactAccumulator(InvestigationFacts facts) {
            values.put(AgentToolbox.FactType.CUSTOMER, facts.customer());
            values.put(AgentToolbox.FactType.ASSET_POLICY, facts.assetPolicy());
            values.put(AgentToolbox.FactType.ADDRESS_RISK, facts.addressRisk());
            values.put(AgentToolbox.FactType.FUNDING, facts.funding());
            values.put(AgentToolbox.FactType.REFERENCE_RATE, facts.referenceRate());
            values.put(AgentToolbox.FactType.TRAVEL_RULE, facts.travelRule());
            values.put(AgentToolbox.FactType.DUPLICATE, facts.duplicate());
        }

        private void put(AgentToolbox.FactType type, LookupResult<?> value) {
            values.put(Objects.requireNonNull(type), Objects.requireNonNull(value));
        }

        private List<AgentToolbox.FactType> missing() {
            List<AgentToolbox.FactType> result = new ArrayList<AgentToolbox.FactType>();
            for (AgentToolbox.FactType type : AgentToolbox.FactType.values()) if (!resolved(value(type))) result.add(type);
            return result;
        }

        private boolean resolved(List<AgentToolbox.FactType> prerequisites) {
            for (AgentToolbox.FactType type : prerequisites) if (!resolved(value(type))) return false;
            return true;
        }

        private boolean resolved(LookupResult<?> result) {
            return !(result instanceof LookupResult.Unavailable<?>) && !(result instanceof LookupResult.Conflict<?>);
        }

        @SuppressWarnings("unchecked")
        private <T> LookupResult<T> value(AgentToolbox.FactType type) { return (LookupResult<T>) values.get(type); }

        private Optional<LookupResult<ReferenceRate>> referenceRate() {
            return Optional.of(this.<ReferenceRate>value(AgentToolbox.FactType.REFERENCE_RATE));
        }

        private InvestigationFacts toFacts(Order order) {
            return new InvestigationFacts(order, value(AgentToolbox.FactType.CUSTOMER),
                    value(AgentToolbox.FactType.ASSET_POLICY), value(AgentToolbox.FactType.ADDRESS_RISK),
                    value(AgentToolbox.FactType.FUNDING), value(AgentToolbox.FactType.REFERENCE_RATE),
                    value(AgentToolbox.FactType.TRAVEL_RULE), value(AgentToolbox.FactType.DUPLICATE));
        }
    }
}
