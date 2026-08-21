package com.jason.yang.asset.infrastructure.agent.llm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Deterministic view of unresolved facts and tools whose prerequisites are currently satisfied. */
public final class InvestigationPlan {
    private final List<AgentToolbox.FactType> missingFacts;
    private final List<AgentToolDefinition> eligibleTools;

    public InvestigationPlan(List<AgentToolbox.FactType> missingFacts,
                             List<AgentToolDefinition> eligibleTools) {
        this.missingFacts = Collections.unmodifiableList(
                new ArrayList<AgentToolbox.FactType>(missingFacts));
        this.eligibleTools = Collections.unmodifiableList(
                new ArrayList<AgentToolDefinition>(eligibleTools));
    }

    public List<AgentToolbox.FactType> missingFacts() { return missingFacts; }
    public List<AgentToolDefinition> eligibleTools() { return eligibleTools; }
    public boolean complete() { return missingFacts.isEmpty(); }
}
