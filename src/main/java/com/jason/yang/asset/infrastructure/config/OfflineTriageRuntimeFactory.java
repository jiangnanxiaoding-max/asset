package com.jason.yang.asset.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.jason.yang.asset.adapter.input.JacksonOrderParser;
import com.jason.yang.asset.application.service.DefaultInvestigationService;
import com.jason.yang.asset.application.service.GuardedInvestigationService;
import com.jason.yang.asset.application.service.DefaultProcessOrderBatchService;
import com.jason.yang.asset.application.service.DefaultRuleEngine;
import com.jason.yang.asset.application.service.DefaultTriageOrderService;
import com.jason.yang.asset.application.service.TemplateDecisionExplanationService;
import com.jason.yang.asset.application.service.DefaultPostDecisionActionService;
import com.jason.yang.asset.application.model.ExecutionMode;
import com.jason.yang.asset.infrastructure.action.RecordingCaseManagementAdapter;
import com.jason.yang.asset.infrastructure.action.RecordingFundsExecutionGateway;
import com.jason.yang.asset.domain.policy.CoreRules;
import com.jason.yang.asset.domain.service.DefaultDecisionAggregator;
import com.jason.yang.asset.infrastructure.audit.JsonLinesDecisionAuditAdapter;
import com.jason.yang.asset.infrastructure.compliance.LocalTravelRuleAdapter;
import com.jason.yang.asset.infrastructure.event.LoggingDomainEventPublisher;
import com.jason.yang.asset.infrastructure.funding.EmbeddedBlockchainDepositStubAdapter;
import com.jason.yang.asset.infrastructure.funding.EmbeddedFiatReceiptStubAdapter;
import com.jason.yang.asset.infrastructure.funding.UnavailableWalletFundsAdapter;
import com.jason.yang.asset.infrastructure.idempotency.InMemoryFundsEventRegistryAdapter;
import com.jason.yang.asset.infrastructure.idempotency.InMemoryOrderProcessingAdapter;
import com.jason.yang.asset.infrastructure.output.JsonLinesDecisionOutputAdapter;
import com.jason.yang.asset.infrastructure.persistence.InMemoryTriageCaseRepository;
import com.jason.yang.asset.infrastructure.policy.StaticPolicyProvider;
import com.jason.yang.asset.infrastructure.reference.FileAddressRiskAdapter;
import com.jason.yang.asset.infrastructure.reference.FileAssetPolicyAdapter;
import com.jason.yang.asset.infrastructure.reference.FileCustomerProfileAdapter;
import com.jason.yang.asset.infrastructure.reference.FileReferenceRateAdapter;
import com.jason.yang.asset.infrastructure.agent.InMemoryAgentRunTraceAdapter;
import com.jason.yang.asset.infrastructure.agent.llm.AgentBatchBudget;
import com.jason.yang.asset.infrastructure.agent.llm.AgentExecutionPolicy;
import com.jason.yang.asset.infrastructure.agent.llm.EnvironmentLlmAgentClientFactory;
import com.jason.yang.asset.infrastructure.agent.llm.LlmAgentInvestigationAdapter;
import com.jason.yang.asset.infrastructure.agent.llm.PortBackedAgentToolbox;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

/** Composition root for the fully offline exercise runtime. */
public final class OfflineTriageRuntimeFactory {
    public OfflineTriageRuntime create(Path materials, Path auditFile, Instant evaluationTime) {
        Path normalizedMaterials = materials.toAbsolutePath().normalize();
        validateMaterials(normalizedMaterials);
        ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

        FileCustomerProfileAdapter customerPort = new FileCustomerProfileAdapter(
                normalizedMaterials.resolve("customers.json"), mapper);
        FileAssetPolicyAdapter assetPort = new FileAssetPolicyAdapter(
                normalizedMaterials.resolve("assets.json"), mapper);
        FileAddressRiskAdapter riskPort = new FileAddressRiskAdapter(
                normalizedMaterials.resolve("address_risk.json"), mapper, evaluationTime);
        FileReferenceRateAdapter ratePort = new FileReferenceRateAdapter(
                normalizedMaterials.resolve("reference_rates.json"), mapper);
        InMemoryFundsEventRegistryAdapter eventRegistry = new InMemoryFundsEventRegistryAdapter();
        EmbeddedFiatReceiptStubAdapter fiatPort = new EmbeddedFiatReceiptStubAdapter();
        EmbeddedBlockchainDepositStubAdapter blockchainPort = new EmbeddedBlockchainDepositStubAdapter();
        UnavailableWalletFundsAdapter walletPort = new UnavailableWalletFundsAdapter();
        LocalTravelRuleAdapter travelRulePort = new LocalTravelRuleAdapter(new BigDecimal("1000"));
        DefaultInvestigationService deterministicInvestigation = new DefaultInvestigationService(
                customerPort,
                assetPort,
                riskPort,
                fiatPort,
                blockchainPort,
                walletPort,
                ratePort,
                travelRulePort,
                eventRegistry
        );
        InMemoryAgentRunTraceAdapter agentTrace = new InMemoryAgentRunTraceAdapter();
        PortBackedAgentToolbox agentTools = new PortBackedAgentToolbox(
                customerPort, assetPort, riskPort, fiatPort, blockchainPort, walletPort,
                ratePort, travelRulePort, eventRegistry);
        LlmAgentInvestigationAdapter llmEnrichment = new LlmAgentInvestigationAdapter(
                new EnvironmentLlmAgentClientFactory().create(mapper), agentTools,
                AgentExecutionPolicy.demoDefaults(), AgentBatchBudget.demoDefaults(), agentTrace);
        GuardedInvestigationService investigation = new GuardedInvestigationService(
                deterministicInvestigation, llmEnrichment);
        JsonLinesDecisionAuditAdapter audit = new JsonLinesDecisionAuditAdapter(auditFile, mapper);
        DefaultTriageOrderService triage = new DefaultTriageOrderService(
                new StaticPolicyProvider(evaluationTime),
                investigation,
                new DefaultRuleEngine(CoreRules.standard()),
                new DefaultDecisionAggregator(),
                new TemplateDecisionExplanationService(),
                audit,
                new InMemoryTriageCaseRepository(),
                new LoggingDomainEventPublisher(),
                new DefaultPostDecisionActionService(
                        ExecutionMode.DECISION_ONLY,
                        new RecordingCaseManagementAdapter(),
                        new RecordingFundsExecutionGateway()
                ),
                agentTrace
        );
        JacksonOrderParser orderParser = new JacksonOrderParser();
        DefaultProcessOrderBatchService batch = new DefaultProcessOrderBatchService(
                orderParser,
                triage,
                new InMemoryOrderProcessingAdapter(),
                new JsonLinesDecisionOutputAdapter(),
                audit
        );
        return new OfflineTriageRuntime(batch, triage, orderParser);
    }

    private void validateMaterials(Path materials) {
        List<String> required = java.util.Arrays.asList(
                "customers.json", "assets.json", "address_risk.json", "reference_rates.json"
        );
        for (String name : required) {
            Path file = materials.resolve(name);
            if (!Files.isRegularFile(file) || !Files.isReadable(file)) {
                throw new IllegalArgumentException("Required material is not readable: " + file);
            }
        }
    }
}
