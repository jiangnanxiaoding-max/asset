package com.jason.yang.asset.application.service;

import com.jason.yang.asset.application.DecisionExplanationService;
import com.jason.yang.asset.application.RuleEngine;
import com.jason.yang.asset.application.TriageOrderUseCase;
import com.jason.yang.asset.application.TriageCommand;
import com.jason.yang.asset.application.port.DecisionAuditPort;
import com.jason.yang.asset.application.port.DomainEventPublisher;
import com.jason.yang.asset.application.port.InvestigationPort;
import com.jason.yang.asset.application.port.PolicyProvider;
import com.jason.yang.asset.application.model.TriageResult;
import com.jason.yang.asset.application.model.SideEffectSummary;
import com.jason.yang.asset.application.port.PostDecisionActionPort;
import com.jason.yang.asset.domain.Order;
import com.jason.yang.asset.domain.PolicySnapshot;
import com.jason.yang.asset.domain.InvestigationFacts;
import com.jason.yang.asset.domain.RuleResult;
import com.jason.yang.asset.domain.TriageDecision;
import com.jason.yang.asset.domain.TriageCase;
import com.jason.yang.asset.domain.repository.TriageCaseRepository;
import com.jason.yang.asset.domain.service.DecisionAggregator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.List;

/**
 * Application service for one exception-triage use case.
 *
 * <p>It coordinates external fact lookup and auditing while all lifecycle
 * invariants and decision policies remain in the domain layer.</p>
 */
public final class DefaultTriageOrderService implements TriageOrderUseCase {
    private static final Logger log = LoggerFactory.getLogger(DefaultTriageOrderService.class);

    private final PolicyProvider policyProvider;
    private final InvestigationPort investigationPort;
    private final RuleEngine ruleEngine;
    private final DecisionAggregator decisionAggregator;
    private final DecisionExplanationService explanationService;
    private final DecisionAuditPort auditPort;
    private final TriageCaseRepository triageCaseRepository;
    private final DomainEventPublisher domainEventPublisher;
    private final PostDecisionActionPort postDecisionActionPort;

    public DefaultTriageOrderService(
            PolicyProvider policyProvider,
            InvestigationPort investigationPort,
            RuleEngine ruleEngine,
            DecisionAggregator decisionAggregator,
            DecisionExplanationService explanationService,
            DecisionAuditPort auditPort,
            TriageCaseRepository triageCaseRepository,
            DomainEventPublisher domainEventPublisher
    ) {
        this(policyProvider, investigationPort, ruleEngine, decisionAggregator,
                explanationService, auditPort, triageCaseRepository, domainEventPublisher,
                ignored -> SideEffectSummary.none());
    }

    public DefaultTriageOrderService(
            PolicyProvider policyProvider,
            InvestigationPort investigationPort,
            RuleEngine ruleEngine,
            DecisionAggregator decisionAggregator,
            DecisionExplanationService explanationService,
            DecisionAuditPort auditPort,
            TriageCaseRepository triageCaseRepository,
            DomainEventPublisher domainEventPublisher,
            PostDecisionActionPort postDecisionActionPort
    ) {
        this.policyProvider = Objects.requireNonNull(policyProvider);
        this.investigationPort = Objects.requireNonNull(investigationPort);
        this.ruleEngine = Objects.requireNonNull(ruleEngine);
        this.decisionAggregator = Objects.requireNonNull(decisionAggregator);
        this.explanationService = Objects.requireNonNull(explanationService);
        this.auditPort = Objects.requireNonNull(auditPort);
        this.triageCaseRepository = Objects.requireNonNull(triageCaseRepository);
        this.domainEventPublisher = Objects.requireNonNull(domainEventPublisher);
        this.postDecisionActionPort = Objects.requireNonNull(postDecisionActionPort);
    }

    /** Runs the deterministic triage pipeline and returns only after audit persistence succeeds. */
    @Override
    public TriageResult triage(TriageCommand command) {
        Objects.requireNonNull(command, "command");
        Order order = command.order();
        Objects.requireNonNull(order, "order");
        TriageCase triageCase = TriageCase.open(order);
        log.info("triage started orderId={} customerId={}",
                order.identity().value(), order.customerIdentity().value());
//    订单
//    → 创建案件聚合
//    → 调查事实
//    → 执行规则
//    → 汇总决定
        triageCase.beginInvestigation();
        PolicySnapshot policy = policyProvider.currentPolicy();
        InvestigationFacts facts = investigationPort.investigate(order, policy);
        List<RuleResult> ruleResults = ruleEngine.evaluateAll(facts, policy);
        TriageDecision decision = decisionAggregator.aggregate(order, ruleResults, policy);
        triageCase.recordDecision(decision);
        triageCaseRepository.save(triageCase);

        if (!decision.fundsMovementAllowed()) {
            log.warn("triage blocked funds movement orderId={} disposition={} reasonCount={}",
                    order.identity().value(), decision.disposition(), decision.reasonCodes().size());
        }

        String explanation = explanationService.explain(decision, ruleResults);
        final String auditId;
        try {
            auditId = auditPort.append(
                    new DecisionAuditPort.AuditContext(
                            command.runId(), command.payloadSha256(), command.sourcePosition()),
                    decision, facts, ruleResults, explanation);
        } catch (RuntimeException exception) {
            log.error("triage audit failed orderId={} disposition={}",
                    order.identity().value(), decision.disposition(), exception);
            throw exception;
        }

        triageCase.markAudited(auditId);
        triageCaseRepository.save(triageCase);
        domainEventPublisher.publish(triageCase.domainEvents());
        SideEffectSummary sideEffects = postDecisionActionPort.handle(triageCase);

        log.info("triage completed orderId={} disposition={} auditId={} ruleCount={}",
                order.identity().value(), decision.disposition(), auditId, ruleResults.size());
        return new TriageResult(decision, explanation, auditId, ruleResults, sideEffects);
    }
}
