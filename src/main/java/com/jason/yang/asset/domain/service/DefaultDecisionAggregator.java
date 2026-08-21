package com.jason.yang.asset.domain.service;

import com.jason.yang.asset.domain.Disposition;
import com.jason.yang.asset.domain.DecisionId;
import com.jason.yang.asset.domain.Order;
import com.jason.yang.asset.domain.PolicySnapshot;
import com.jason.yang.asset.domain.ReasonCode;
import com.jason.yang.asset.domain.RuleResult;
import com.jason.yang.asset.domain.TriageDecision;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.Collections;
import java.util.function.Supplier;

/** Domain service implementing the fail-closed disposition precedence. */
public final class DefaultDecisionAggregator implements DecisionAggregator {
    private final Supplier<String> decisionIdSupplier;

    public DefaultDecisionAggregator() {
        this(() -> UUID.randomUUID().toString());
    }

    public DefaultDecisionAggregator(Supplier<String> decisionIdSupplier) {
        this.decisionIdSupplier = decisionIdSupplier;
    }

    @Override
    public TriageDecision aggregate(
            Order order,
            List<RuleResult> ruleResults,
            PolicySnapshot policy
    ) {
        Disposition disposition = Disposition.AUTO_COMPLETE;
        List<ReasonCode> reasons = new ArrayList<ReasonCode>();
        HashSet<ReasonCode> seenReasons = new HashSet<ReasonCode>();
        for (RuleResult result : ruleResults) {
            if (result.passed()) continue;
            if (result.proposedDisposition().isPresent()
                    && priority(result.proposedDisposition().get()) > priority(disposition)) {
                disposition = result.proposedDisposition().get();
            }
            if (result.reasonCode().isPresent() && seenReasons.add(result.reasonCode().get())) {
                reasons.add(result.reasonCode().get());
            }
        }

        if (reasons.isEmpty()) {
            reasons = Collections.singletonList(ReasonCode.ALL_CHECKS_PASSED);
        }

        return new TriageDecision(
                new DecisionId(decisionIdSupplier.get()),
                order.identity(),
                disposition,
                reasons,
                disposition == Disposition.AUTO_COMPLETE,
                policy.version(),
                policy.evaluationTime()
        );
    }

    private int priority(Disposition disposition) {
        switch (disposition) {
            case FREEZE_COMPLIANCE: return 100;
            case INVALID_INPUT: return 95;
            case REJECT_ESCALATE: return 90;
            case HOLD: return 80;
            case DUPLICATE_NOOP: return 70;
            case OPS_RECOVERY: return 60;
            case MANUAL_REVIEW: return 50;
            case REFUND_REVIEW: return 45;
            case REQUOTE: return 40;
            default: return 0;
        }
    }
}
