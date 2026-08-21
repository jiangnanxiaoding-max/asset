package com.jason.yang.asset.application.port;

import com.jason.yang.asset.domain.InvestigationFacts;
import com.jason.yang.asset.domain.Order;
import com.jason.yang.asset.domain.PolicySnapshot;

/** Optionally enriches unresolved authoritative facts without making a disposition. */
public interface AgentEnrichmentPort {
    InvestigationFacts enrich(Order order, PolicySnapshot policy, InvestigationFacts currentFacts);
}
