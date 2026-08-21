package com.jason.yang.asset.application.service;

import com.jason.yang.asset.domain.policy.CoreRules;
import com.jason.yang.asset.domain.service.DefaultDecisionAggregator;
import com.jason.yang.asset.domain.AddressRiskAssessment;
import com.jason.yang.asset.domain.AssetNetworkPolicy;
import com.jason.yang.asset.domain.BankPayout;
import com.jason.yang.asset.domain.CounterpartyInfo;
import com.jason.yang.asset.domain.CustomerProfile;
import com.jason.yang.asset.domain.DepositReference;
import com.jason.yang.asset.domain.Disposition;
import com.jason.yang.asset.domain.DuplicateAssessment;
import com.jason.yang.asset.domain.FundingEvidence;
import com.jason.yang.asset.domain.InvestigationFacts;
import com.jason.yang.asset.domain.LookupResult;
import com.jason.yang.asset.domain.OffRampOrder;
import com.jason.yang.asset.domain.OnRampOrder;
import com.jason.yang.asset.domain.Order;
import com.jason.yang.asset.domain.PolicySnapshot;
import com.jason.yang.asset.domain.ReasonCode;
import com.jason.yang.asset.domain.ReferenceRate;
import com.jason.yang.asset.domain.TravelRuleAssessment;
import com.jason.yang.asset.domain.TriageDecision;
import com.jason.yang.asset.domain.RuleResult;
import com.jason.yang.asset.domain.WithdrawalOrder;
import com.jason.yang.asset.application.model.TriageResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CoreDecisionServiceTest {
    private static final Instant NOW = Instant.parse("2026-07-28T12:00:00Z");
    private static final PolicySnapshot POLICY = new PolicySnapshot(
            "policy-2026-07-28",
            NOW,
            new BigDecimal("0.01"),
            new BigDecimal("1000")
    );

    private DefaultRuleEngine ruleEngine;
    private DefaultDecisionAggregator aggregator;

    @BeforeEach
    void setUp() {
        ruleEngine = new DefaultRuleEngine(CoreRules.standard());
        aggregator = new DefaultDecisionAggregator(() -> "D-TEST");
    }

    @Test
    void cleanOffRampCanAutoComplete() {
        OffRampOrder order = cleanOffRamp("O-CLEAN");
        InvestigationFacts facts = cleanOffRampFacts(order);

        TriageDecision decision = decide(facts);

        assertEquals(Disposition.AUTO_COMPLETE, decision.disposition());
        assertTrue(decision.fundsMovementAllowed());
        assertEquals(Collections.singletonList(ReasonCode.ALL_CHECKS_PASSED), decision.reasonCodes());
    }

    @Test
    void sanctionedAddressOverridesOtherOutcomes() {
        OffRampOrder order = cleanOffRamp("O-SANCTIONED");
        InvestigationFacts base = cleanOffRampFacts(order);
        InvestigationFacts facts = withRisk(base, new AddressRiskAssessment(
                99,
                AddressRiskAssessment.RiskCategory.SANCTIONED,
                NOW,
                "risk:sanctioned"
        ));

        TriageDecision decision = decide(facts);

        assertEquals(Disposition.FREEZE_COMPLIANCE, decision.disposition());
        assertFalse(decision.fundsMovementAllowed());
        assertTrue(decision.reasonCodes().contains(ReasonCode.ADDRESS_SANCTIONED));
    }

    @Test
    void insufficientConfirmationsMustHold() {
        OffRampOrder order = cleanOffRamp("O-CONFIRMATIONS");
        InvestigationFacts base = cleanOffRampFacts(order);
        InvestigationFacts facts = withFunding(base, new FundingEvidence.Chain(
                FundingEvidence.Status.CONFIRMED,
                "ERC20:0xtest:0",
                "ERC20",
                new BigDecimal("1000"),
                3
        ));

        TriageDecision decision = decide(facts);

        assertEquals(Disposition.HOLD, decision.disposition());
        assertTrue(decision.reasonCodes().contains(ReasonCode.INSUFFICIENT_CONFIRMATIONS));
    }

    @Test
    void duplicateTransactionMustBeNoOp() {
        OffRampOrder order = cleanOffRamp("O-DUPLICATE");
        InvestigationFacts base = cleanOffRampFacts(order);
        InvestigationFacts facts = withDuplicate(base, new DuplicateAssessment(
                DuplicateAssessment.Status.ALREADY_CREDITED,
                "O-FIRST"
        ));

        TriageDecision decision = decide(facts);

        assertEquals(Disposition.DUPLICATE_NOOP, decision.disposition());
        assertFalse(decision.fundsMovementAllowed());
        assertTrue(decision.reasonCodes().contains(ReasonCode.DUPLICATE_TX_HASH));
    }

    @Test
    void unknownAddressMustFailClosed() {
        OffRampOrder order = cleanOffRamp("O-UNKNOWN-ADDRESS");
        InvestigationFacts base = cleanOffRampFacts(order);
        InvestigationFacts facts = new InvestigationFacts(
                order,
                base.customer(),
                base.assetPolicy(),
                LookupResult.notFound("ADDRESS_NOT_FOUND"),
                base.funding(),
                base.referenceRate(),
                base.travelRule(),
                base.duplicate()
        );

        TriageDecision decision = decide(facts);

        assertEquals(Disposition.HOLD, decision.disposition());
        assertTrue(decision.reasonCodes().contains(ReasonCode.ADDRESS_UNKNOWN));
    }

    @Test
    void expiredQuoteOutsideToleranceMustRequote() {
        OnRampOrder order = new OnRampOrder(
                "O-EXPIRED",
                "c001",
                "BTC",
                "BTC",
                new BigDecimal("1000"),
                new BigDecimal("0.0152"),
                Instant.parse("2026-07-28T11:30:00Z"),
                "0xCLEAN01",
                CounterpartyInfo.directCustomer(),
                ""
        );
        InvestigationFacts facts = new InvestigationFacts(
                order,
                activeCustomer("c001", "陈爱丽", "50000"),
                LookupResult.found(new AssetNetworkPolicy(
                        "BTC", "BTC", new BigDecimal("0.0005"), 3, 8, RoundingMode.DOWN
                ), "asset:BTC:BTC"),
                cleanRisk(),
                LookupResult.found(new FundingEvidence.Fiat(
                        FundingEvidence.Status.CONFIRMED, new BigDecimal("1000")
                ), "fiat:O-EXPIRED"),
                LookupResult.found(new ReferenceRate(
                        "BTC", "USD", new BigDecimal("67000"), NOW, "reference-rates"
                ), "rate:BTC/USD"),
                LookupResult.found(TravelRuleAssessment.notRequired(), "travel:not-required"),
                LookupResult.notApplicable()
        );

        TriageDecision decision = decide(facts);

        assertEquals(Disposition.REQUOTE, decision.disposition());
        assertTrue(decision.reasonCodes().contains(ReasonCode.QUOTE_SLIPPAGE_EXCEEDED));
    }

    @Test
    void thirdPartyBankAccountMustRejectAndEscalate() {
        OffRampOrder order = new OffRampOrder(
                "O-BANK",
                "c001",
                "USDT",
                "ERC20",
                new BigDecimal("1000"),
                Instant.parse("2026-07-28T12:05:00Z"),
                new DepositReference("0xtest", "0", "0xCLEAN01", "ERC20"),
                new BankPayout("陈氏贸易有限公司", "USD", new BigDecimal("990")),
                CounterpartyInfo.directCustomer(),
                ""
        );
        InvestigationFacts facts = cleanOffRampFacts(order);

        TriageDecision decision = decide(facts);

        assertEquals(Disposition.REJECT_ESCALATE, decision.disposition());
        assertTrue(decision.reasonCodes().contains(ReasonCode.BANK_NAME_MISMATCH));
    }

    @Test
    void missingTravelRuleInformationMustHold() {
        WithdrawalOrder order = new WithdrawalOrder(
                "O-TRAVEL",
                "c003",
                "BTC",
                "BTC",
                new BigDecimal("0.5"),
                "0xCLEAN02",
                new CounterpartyInfo(
                        CounterpartyInfo.VaspStatus.VASP,
                        "unknown",
                        true,
                        false
                ),
                ""
        );
        InvestigationFacts facts = new InvestigationFacts(
                order,
                activeCustomer("c003", "李卡罗", "500000"),
                LookupResult.found(new AssetNetworkPolicy(
                        "BTC", "BTC", new BigDecimal("0.0005"), 3, 8, RoundingMode.DOWN
                ), "asset:BTC:BTC"),
                cleanRisk(),
                LookupResult.found(new FundingEvidence.Wallet(
                        FundingEvidence.Status.RESERVED,
                        new BigDecimal("2"),
                        new BigDecimal("0.5")
                ), "wallet:reservation"),
                LookupResult.found(new ReferenceRate(
                        "BTC", "USD", new BigDecimal("67000"), NOW, "reference-rates"
                ), "rate:BTC/USD"),
                LookupResult.found(new TravelRuleAssessment(
                        true, true, false, Collections.singletonList("beneficiary")
                ), "travel:O-TRAVEL"),
                LookupResult.notApplicable()
        );

        TriageDecision decision = decide(facts);

        assertEquals(Disposition.HOLD, decision.disposition());
        assertTrue(decision.reasonCodes().contains(ReasonCode.TRAVEL_RULE_INFO_MISSING));
    }

    @Test
    void triageServiceRunsPipelineAndPersistsAuditBeforeReturning() {
        OffRampOrder order = cleanOffRamp("O-PIPELINE");
        InvestigationFacts facts = cleanOffRampFacts(order);
        AtomicBoolean audited = new AtomicBoolean();
        DefaultTriageOrderService service = new DefaultTriageOrderService(
                () -> POLICY,
                (ignoredOrder, ignoredPolicy) -> facts,
                ruleEngine,
                aggregator,
                new TemplateDecisionExplanationService(),
                (decision, auditFacts, ruleResults, explanation) -> {
                    audited.set(true);
                    assertEquals(order.orderId(), decision.orderId().value());
                    assertFalse(explanation.trim().isEmpty());
                    return "A-TEST";
                },
                ignoredCase -> { },
                ignoredEvents -> { }
        );

        TriageResult result = service.triage(order);

        assertTrue(audited.get());
        assertEquals("A-TEST", result.auditId());
        assertEquals(Disposition.AUTO_COMPLETE, result.decision().disposition());
    }

    @Test
    void auditFailurePreventsEveryPostDecisionSideEffect() {
        OffRampOrder order = cleanOffRamp("O-AUDIT-FAIL");
        InvestigationFacts facts = cleanOffRampFacts(order);
        AtomicBoolean sideEffectCalled = new AtomicBoolean();
        DefaultTriageOrderService service = new DefaultTriageOrderService(
                () -> POLICY,
                (ignoredOrder, ignoredPolicy) -> facts,
                ruleEngine,
                aggregator,
                new TemplateDecisionExplanationService(),
                (decision, auditFacts, ruleResults, explanation) -> {
                    throw new IllegalStateException("audit unavailable");
                },
                ignoredCase -> { },
                ignoredEvents -> { },
                ignoredCase -> {
                    sideEffectCalled.set(true);
                    return com.jason.yang.asset.application.model.SideEffectSummary.none();
                }
        );

        assertThrows(IllegalStateException.class, () -> service.triage(order));
        assertFalse(sideEffectCalled.get());
    }

    private com.jason.yang.asset.domain.TriageDecision decide(InvestigationFacts facts) {
        List<RuleResult> results = ruleEngine.evaluateAll(facts, POLICY);
        return aggregator.aggregate(facts.order(), results, POLICY);
    }

    private OffRampOrder cleanOffRamp(String orderId) {
        return new OffRampOrder(
                orderId,
                "c001",
                "USDT",
                "ERC20",
                new BigDecimal("1000"),
                Instant.parse("2026-07-28T12:05:00Z"),
                new DepositReference("0xtest", "0", "0xCLEAN01", "ERC20"),
                new BankPayout("陈爱丽", "USD", new BigDecimal("990")),
                CounterpartyInfo.directCustomer(),
                ""
        );
    }

    private InvestigationFacts cleanOffRampFacts(OffRampOrder order) {
        return new InvestigationFacts(
                order,
                activeCustomer("c001", "陈爱丽", "50000"),
                LookupResult.found(new AssetNetworkPolicy(
                        "USDT", "ERC20", new BigDecimal("20"), 12, 6, RoundingMode.DOWN
                ), "asset:USDT:ERC20"),
                cleanRisk(),
                LookupResult.found(new FundingEvidence.Chain(
                        FundingEvidence.Status.CONFIRMED,
                        "ERC20:0xtest:0",
                        "ERC20",
                        new BigDecimal("1000"),
                        15
                ), "chain:0xtest"),
                LookupResult.notApplicable(),
                LookupResult.found(TravelRuleAssessment.notRequired(), "travel:not-required"),
                LookupResult.found(new DuplicateAssessment(
                        DuplicateAssessment.Status.NEW, ""
                ), "event:new")
        );
    }

    private LookupResult<CustomerProfile> activeCustomer(
            String customerId,
            String name,
            String limit
    ) {
        return LookupResult.found(new CustomerProfile(
                customerId,
                name,
                2,
                new BigDecimal(limit),
                Optional.empty(),
                name,
                CustomerProfile.Status.ACTIVE
        ), "customer:" + customerId);
    }

    private LookupResult<AddressRiskAssessment> cleanRisk() {
        return LookupResult.found(new AddressRiskAssessment(
                5,
                AddressRiskAssessment.RiskCategory.CLEAN,
                NOW,
                "risk:clean"
        ), "risk:clean");
    }

    private InvestigationFacts withRisk(
            InvestigationFacts facts,
            AddressRiskAssessment risk
    ) {
        return new InvestigationFacts(
                facts.order(), facts.customer(), facts.assetPolicy(),
                LookupResult.found(risk, "risk:override"),
                facts.funding(), facts.referenceRate(), facts.travelRule(), facts.duplicate()
        );
    }

    private InvestigationFacts withFunding(
            InvestigationFacts facts,
            FundingEvidence funding
    ) {
        return new InvestigationFacts(
                facts.order(), facts.customer(), facts.assetPolicy(), facts.addressRisk(),
                LookupResult.found(funding, "funding:override"),
                facts.referenceRate(), facts.travelRule(), facts.duplicate()
        );
    }

    private InvestigationFacts withDuplicate(
            InvestigationFacts facts,
            DuplicateAssessment duplicate
    ) {
        return new InvestigationFacts(
                facts.order(), facts.customer(), facts.assetPolicy(), facts.addressRisk(),
                facts.funding(), facts.referenceRate(), facts.travelRule(),
                LookupResult.found(duplicate, "duplicate:override")
        );
    }
}
