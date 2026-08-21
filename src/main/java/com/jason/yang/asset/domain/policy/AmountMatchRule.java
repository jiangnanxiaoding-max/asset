package com.jason.yang.asset.domain.policy;

import com.jason.yang.asset.domain.Disposition;
import com.jason.yang.asset.domain.FundingEvidence;
import com.jason.yang.asset.domain.InvestigationFacts;
import com.jason.yang.asset.domain.LookupResult;
import com.jason.yang.asset.domain.OffRampOrder;
import com.jason.yang.asset.domain.OnRampOrder;
import com.jason.yang.asset.domain.PolicySnapshot;
import com.jason.yang.asset.domain.ReasonCode;
import com.jason.yang.asset.domain.RuleResult;
import com.jason.yang.asset.domain.WithdrawalOrder;

public final class AmountMatchRule implements TriageRule {
    @Override
    public String id() {
        return "AMOUNT_MATCH";
    }

    @Override
    public RuleResult evaluate(InvestigationFacts facts, PolicySnapshot policy) {
        if (!(facts.funding() instanceof LookupResult.Found)) {
            return RuleResult.pass(id());
        }
        LookupResult.Found<FundingEvidence> found =
                (LookupResult.Found<FundingEvidence>) facts.funding();

        boolean matches = false;
        if (facts.order() instanceof OnRampOrder && found.data() instanceof FundingEvidence.Fiat) {
            OnRampOrder order = (OnRampOrder) facts.order();
            FundingEvidence.Fiat evidence = (FundingEvidence.Fiat) found.data();
            matches = evidence.receivedAmountUsd().compareTo(order.fiatAmountUsd()) == 0;
        } else if (facts.order() instanceof OffRampOrder && found.data() instanceof FundingEvidence.Chain) {
            OffRampOrder order = (OffRampOrder) facts.order();
            FundingEvidence.Chain evidence = (FundingEvidence.Chain) found.data();
            matches = evidence.observedAmount().compareTo(order.quotedCryptoAmount()) == 0;
        } else if (facts.order() instanceof WithdrawalOrder && found.data() instanceof FundingEvidence.Wallet) {
            WithdrawalOrder order = (WithdrawalOrder) facts.order();
            FundingEvidence.Wallet evidence = (FundingEvidence.Wallet) found.data();
            matches = evidence.reservedAmount().compareTo(order.amount()) >= 0;
        }

        if (!matches) {
            return RuleResult.fail(id(), Disposition.MANUAL_REVIEW,
                    ReasonCode.AMOUNT_MISMATCH,
                    "确认到账或预留金额与订单要求金额不匹配");
        }
        return RuleResult.pass(id());
    }
}
