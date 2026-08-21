package com.jason.yang.asset.infrastructure.agent.llm;

import com.jason.yang.asset.domain.Order;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** Runtime tool schema supplied to the model, including applicability and fact prerequisites. */
public final class AgentToolDefinition {
    private final String name;
    private final String description;
    private final AgentToolbox.FactType producesFact;
    private final List<String> applicableOrderTypes;
    private final List<AgentToolbox.FactType> prerequisites;

    public AgentToolDefinition(String name, String description, AgentToolbox.FactType producesFact,
                               List<String> applicableOrderTypes,
                               List<AgentToolbox.FactType> prerequisites) {
        this.name = Objects.requireNonNull(name);
        this.description = Objects.requireNonNull(description);
        this.producesFact = Objects.requireNonNull(producesFact);
        this.applicableOrderTypes = Collections.unmodifiableList(new ArrayList<String>(applicableOrderTypes));
        this.prerequisites = Collections.unmodifiableList(
                new ArrayList<AgentToolbox.FactType>(prerequisites));
    }

    public String name() { return name; }
    public String getName() { return name; }
    public String description() { return description; }
    public String getDescription() { return description; }
    public AgentToolbox.FactType producesFact() { return producesFact; }
    public AgentToolbox.FactType getProducesFact() { return producesFact; }
    public List<String> applicableOrderTypes() { return applicableOrderTypes; }
    public List<String> getApplicableOrderTypes() { return applicableOrderTypes; }
    public List<AgentToolbox.FactType> prerequisites() { return prerequisites; }
    public List<AgentToolbox.FactType> getPrerequisites() { return prerequisites; }

    public boolean appliesTo(Order order) {
        return applicableOrderTypes.isEmpty()
                || applicableOrderTypes.contains(order.getClass().getSimpleName());
    }
}
