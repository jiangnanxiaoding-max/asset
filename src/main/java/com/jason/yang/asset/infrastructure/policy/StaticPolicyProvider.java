package com.jason.yang.asset.infrastructure.policy;

import com.jason.yang.asset.application.port.PolicyProvider;
import com.jason.yang.asset.domain.PolicySnapshot;

import java.math.BigDecimal;
import java.time.Instant;

/** Versioned deterministic policy used by the supplied exercise snapshot. */
public final class StaticPolicyProvider implements PolicyProvider {
    private final PolicySnapshot policy;

    public StaticPolicyProvider(Instant evaluationTime) {
        this.policy = new PolicySnapshot(
                "policy-2026-07-28",
                evaluationTime,
                new BigDecimal("0.01"),
                new BigDecimal("1000")
        );
    }

    @Override
    public PolicySnapshot currentPolicy() {
        return policy;
    }
}
