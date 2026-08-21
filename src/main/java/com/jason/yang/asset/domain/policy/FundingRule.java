package com.jason.yang.asset.domain.policy;

import com.jason.yang.asset.domain.AssetNetworkPolicy;
import com.jason.yang.asset.domain.Disposition;
import com.jason.yang.asset.domain.FundingEvidence;
import com.jason.yang.asset.domain.InvestigationFacts;
import com.jason.yang.asset.domain.LookupResult;
import com.jason.yang.asset.domain.PolicySnapshot;
import com.jason.yang.asset.domain.ReasonCode;
import com.jason.yang.asset.domain.RuleResult;
import com.jason.yang.asset.domain.WithdrawalOrder;

public final class FundingRule implements TriageRule {
    @Override
    public String id() {
        return "FUNDING_AND_CONFIRMATIONS";
    }

    @Override
    @SuppressWarnings("unchecked")
    public RuleResult evaluate(InvestigationFacts facts, PolicySnapshot policy) {
        if (facts.funding() instanceof LookupResult.NotFound) {
            return notConfirmed("No trusted funding or reservation record was found");
        }
        if (!(facts.funding() instanceof LookupResult.Found)) {
            return RuleResult.pass(id());
        }

        LookupResult.Found<FundingEvidence> found =
                (LookupResult.Found<FundingEvidence>) facts.funding();
        FundingEvidence evidence = found.data();
        if (evidence instanceof FundingEvidence.Fiat) {
            FundingEvidence.Fiat fiat = (FundingEvidence.Fiat) evidence;
            return fiat.status() == FundingEvidence.Status.CONFIRMED
                    ? RuleResult.pass(id())
                    : notConfirmed("Fiat status is " + fiat.status());
        }
        if (evidence instanceof FundingEvidence.Chain) {
            return evaluateChain(facts, (FundingEvidence.Chain) evidence);
        }
        return evaluateWallet(facts, (FundingEvidence.Wallet) evidence);
    }

    @SuppressWarnings("unchecked")
    private RuleResult evaluateChain(InvestigationFacts facts, FundingEvidence.Chain chain) {
        if (chain.status() != FundingEvidence.Status.CONFIRMED) {
            return notConfirmed("Blockchain deposit status is " + chain.status());
        }
        if (facts.assetPolicy() instanceof LookupResult.Found) {
            LookupResult.Found<AssetNetworkPolicy> policy =
                    (LookupResult.Found<AssetNetworkPolicy>) facts.assetPolicy();
            if (chain.confirmations() < policy.data().confirmationsRequired()) {
                return RuleResult.fail(id(), Disposition.HOLD,
                        ReasonCode.INSUFFICIENT_CONFIRMATIONS,
                        "Confirmations " + chain.confirmations()
                                + " are below required " + policy.data().confirmationsRequired());
            }
        }
        return RuleResult.pass(id());
    }

    private RuleResult evaluateWallet(InvestigationFacts facts, FundingEvidence.Wallet wallet) {
        if (!(facts.order() instanceof WithdrawalOrder)) {
            return RuleResult.fail(id(), Disposition.HOLD,
                    ReasonCode.WALLET_FUNDS_NOT_RESERVED, "Withdrawal order is required");
        }
        WithdrawalOrder withdrawal = (WithdrawalOrder) facts.order();
        if (wallet.status() != FundingEvidence.Status.RESERVED
                || wallet.reservedAmount().compareTo(withdrawal.amount()) < 0) {
            return RuleResult.fail(id(), Disposition.HOLD,
                    ReasonCode.WALLET_FUNDS_NOT_RESERVED,
                    "Wallet funds have not been sufficiently reserved for this withdrawal");
        }
        return RuleResult.pass(id());
    }

    private RuleResult notConfirmed(String detail) {
        return RuleResult.fail(id(), Disposition.HOLD,
                ReasonCode.FUNDING_NOT_CONFIRMED, detail);
    }
}
