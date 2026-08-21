package com.jason.yang.asset.application.service;

import com.jason.yang.asset.application.port.AgentEnrichmentPort;
import com.jason.yang.asset.application.port.InvestigationPort;
import com.jason.yang.asset.domain.InvestigationFacts;
import com.jason.yang.asset.domain.LookupResult;
import com.jason.yang.asset.domain.Order;
import com.jason.yang.asset.domain.PolicySnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Objects;

/** Runs cheap deterministic lookups first and invokes Agent enrichment only for unresolved facts. */
public class GuardedInvestigationService implements InvestigationPort {
    private static final Logger log = LoggerFactory.getLogger(GuardedInvestigationService.class);
    private final InvestigationPort deterministicInvestigation;
    private final AgentEnrichmentPort enrichmentPort;

    public GuardedInvestigationService(
            InvestigationPort deterministicInvestigation,
            AgentEnrichmentPort enrichmentPort
    ) {
        this.deterministicInvestigation = Objects.requireNonNull(deterministicInvestigation);
        this.enrichmentPort = Objects.requireNonNull(enrichmentPort);
    }

    @Override
    public InvestigationFacts investigate(Order order, PolicySnapshot policy) {
        InvestigationFacts facts = deterministicInvestigation.investigate(order, policy);
        if (!hasUnresolvedFacts(facts)) {
            return facts;
        }
        log.info("agent enrichment requested orderId={} reason=UNRESOLVED_FACTS", order.orderId());
        return enrichmentPort.enrich(order, policy, facts);
    }

    private boolean hasUnresolvedFacts(InvestigationFacts facts) {
        return Arrays.<LookupResult<?>>asList(
                facts.customer(), facts.assetPolicy(), facts.addressRisk(), facts.funding(),
                facts.referenceRate(), facts.travelRule(), facts.duplicate())
                .stream()
                .anyMatch(this::unresolved);
    }

    private boolean unresolved(LookupResult<?> result) {
        return result instanceof LookupResult.Unavailable<?>
                || result instanceof LookupResult.Conflict<?>;
    }
}
