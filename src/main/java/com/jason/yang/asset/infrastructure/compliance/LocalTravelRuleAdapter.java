package com.jason.yang.asset.infrastructure.compliance;

import com.jason.yang.asset.application.port.TravelRulePort;
import com.jason.yang.asset.domain.CounterpartyInfo;
import com.jason.yang.asset.domain.LookupResult;
import com.jason.yang.asset.domain.Order;
import com.jason.yang.asset.domain.TravelRuleAssessment;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** Deterministic Travel Rule evaluator; no language model is involved. */
public final class LocalTravelRuleAdapter implements TravelRulePort {
    private final BigDecimal thresholdUsd;

    public LocalTravelRuleAdapter(BigDecimal thresholdUsd) {
        this.thresholdUsd = thresholdUsd;
    }

    @Override
    public LookupResult<TravelRuleAssessment> assess(Order order, BigDecimal usdEquivalent) {
        if (order.counterparty().vaspStatus() != CounterpartyInfo.VaspStatus.VASP
                || usdEquivalent.compareTo(thresholdUsd) < 0) {
            return LookupResult.found(TravelRuleAssessment.notRequired(), "travel-rule:not-required");
        }
        List<String> missing = new ArrayList<String>();
        if (!order.counterparty().originatorComplete()) {
            missing.add("originator");
        }
        if (!order.counterparty().beneficiaryComplete()) {
            missing.add("beneficiary");
        }
        return LookupResult.found(new TravelRuleAssessment(
                true,
                order.counterparty().originatorComplete(),
                order.counterparty().beneficiaryComplete(),
                missing
        ), "travel-rule:" + order.orderId());
    }
}
