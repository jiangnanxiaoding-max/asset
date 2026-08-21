package com.jason.yang.asset.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TriageCaseTest {
    private static final Instant NOW = Instant.parse("2026-07-28T12:00:00Z");

    @Test
    void cannotRecordDecisionBeforeInvestigationStarts() {
        TriageCase triageCase = TriageCase.open(order());

        assertThrows(IllegalStateException.class, () -> triageCase.recordDecision(autoDecision()));
    }

    @Test
    void cannotBecomeExecutionEligibleBeforeAudit() {
        TriageCase triageCase = TriageCase.open(order());
        triageCase.beginInvestigation();
        triageCase.recordDecision(autoDecision());

        assertFalse(triageCase.fundsMovementEligible());

        triageCase.markAudited("A-001");

        assertTrue(triageCase.fundsMovementEligible());
        assertEquals(TriageCase.Status.AUDITED, triageCase.status());
    }

    @Test
    void complianceFreezeEmitsDedicatedDomainEventAndNeverAllowsFundsMovement() {
        TriageCase triageCase = TriageCase.open(order());
        TriageDecision freeze = new TriageDecision(
                "D-FREEZE",
                order().identity(),
                Disposition.FREEZE_COMPLIANCE,
                Collections.singletonList(ReasonCode.ADDRESS_SANCTIONED),
                false,
                "policy-test",
                NOW
        );

        triageCase.beginInvestigation();
        triageCase.recordDecision(freeze);
        triageCase.markAudited("A-FREEZE");

        assertFalse(triageCase.fundsMovementEligible());
        assertEquals(2, triageCase.domainEvents().size());
        assertTrue(triageCase.domainEvents().stream()
                .anyMatch(ComplianceFreezeRequired.class::isInstance));
    }

    @Test
    void invalidDomainAmountsAreRejectedAtConstruction() {
        assertThrows(IllegalArgumentException.class, () -> new OnRampOrder(
                "O-BAD",
                "c001",
                "USDT",
                "ERC20",
                BigDecimal.ZERO,
                new BigDecimal("100"),
                NOW,
                "0xCLEAN",
                CounterpartyInfo.unknown(),
                ""
        ));
        assertThrows(IllegalArgumentException.class, () -> new RiskScore(101));
    }

    private OnRampOrder order() {
        return new OnRampOrder(
                "O-AGGREGATE",
                "c001",
                "USDT",
                "ERC20",
                new BigDecimal("1000"),
                new BigDecimal("1000"),
                Instant.parse("2026-07-28T12:05:00Z"),
                "0xCLEAN01",
                CounterpartyInfo.unknown(),
                ""
        );
    }

    private TriageDecision autoDecision() {
        return new TriageDecision(
                "D-AUTO",
                order().identity(),
                Disposition.AUTO_COMPLETE,
                Collections.singletonList(ReasonCode.ALL_CHECKS_PASSED),
                true,
                "policy-test",
                NOW
        );
    }
}
