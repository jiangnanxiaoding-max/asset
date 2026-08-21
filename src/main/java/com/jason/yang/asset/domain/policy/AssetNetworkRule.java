package com.jason.yang.asset.domain.policy;

import com.jason.yang.asset.domain.AssetNetworkPolicy;
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

import java.math.BigDecimal;

public final class AssetNetworkRule implements TriageRule {
    @Override
    public String id() {
        return "ASSET_AND_NETWORK";
    }

    @Override
    public RuleResult evaluate(InvestigationFacts facts, PolicySnapshot policy) {
        if (facts.assetPolicy() instanceof LookupResult.NotFound<?>) {
            return RuleResult.fail(id(), Disposition.OPS_RECOVERY,
                    ReasonCode.UNSUPPORTED_ASSET_OR_NETWORK,
                    "资产或网络不在支持列表中，需运维回收");
        }
        if (!(facts.assetPolicy() instanceof LookupResult.Found)) {
            return RuleResult.pass(id());
        }
        LookupResult.Found<AssetNetworkPolicy> found =
                (LookupResult.Found<AssetNetworkPolicy>) facts.assetPolicy();

        if (facts.order() instanceof OffRampOrder
                && facts.funding().value().orElse(null) instanceof FundingEvidence.Chain
                && !((OffRampOrder) facts.order()).network().equalsIgnoreCase(
                        ((FundingEvidence.Chain) facts.funding().value().orElse(null)).observedNetwork())) {
            OffRampOrder offRamp = (OffRampOrder) facts.order();
            FundingEvidence.Chain chain =
                    (FundingEvidence.Chain) facts.funding().value().orElse(null);
            return RuleResult.fail(id(), Disposition.OPS_RECOVERY,
                    ReasonCode.DEPOSIT_NETWORK_MISMATCH,
                    "订单网络为 " + offRamp.network()
                            + "，实际到账网络为 " + chain.observedNetwork());
        }

        BigDecimal amount;
        if (facts.order() instanceof OnRampOrder) {
            amount = ((OnRampOrder) facts.order()).quotedCryptoAmount();
        } else if (facts.order() instanceof OffRampOrder) {
            amount = ((OffRampOrder) facts.order()).quotedCryptoAmount();
        } else {
            amount = ((WithdrawalOrder) facts.order()).amount();
        }
        if (amount.compareTo(found.data().minimumAmount()) < 0) {
            return RuleResult.fail(id(), Disposition.MANUAL_REVIEW,
                    ReasonCode.BELOW_MINIMUM_AMOUNT,
                    "金额 " + amount + " 低于最小金额 " + found.data().minimumAmount());
        }
        return RuleResult.pass(id());
    }
}
