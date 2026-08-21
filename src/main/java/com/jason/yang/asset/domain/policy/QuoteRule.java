package com.jason.yang.asset.domain.policy;

import com.jason.yang.asset.domain.Disposition;
import com.jason.yang.asset.domain.InvestigationFacts;
import com.jason.yang.asset.domain.LookupResult;
import com.jason.yang.asset.domain.OffRampOrder;
import com.jason.yang.asset.domain.OnRampOrder;
import com.jason.yang.asset.domain.PolicySnapshot;
import com.jason.yang.asset.domain.ReasonCode;
import com.jason.yang.asset.domain.ReferenceRate;
import com.jason.yang.asset.domain.RuleResult;

import java.math.BigDecimal;
import java.math.MathContext;
import java.time.Instant;

public final class QuoteRule implements TriageRule {
    @Override
    public String id() {
        return "QUOTE_EXPIRY_AND_SLIPPAGE";
    }

    @Override
    public RuleResult evaluate(InvestigationFacts facts, PolicySnapshot policy) {
        Instant expiresAt;
        BigDecimal originalUsd;
        BigDecimal quotedCrypto;

        if (facts.order() instanceof OnRampOrder) {
            OnRampOrder onRamp = (OnRampOrder) facts.order();
            expiresAt = onRamp.quoteExpiresAt();
            originalUsd = onRamp.fiatAmountUsd();
            quotedCrypto = onRamp.quotedCryptoAmount();
        } else if (facts.order() instanceof OffRampOrder) {
            OffRampOrder offRamp = (OffRampOrder) facts.order();
            expiresAt = offRamp.quoteExpiresAt();
            if (!"USD".equalsIgnoreCase(offRamp.payout().currency())) {
                return RuleResult.fail(id(), Disposition.HOLD,
                        ReasonCode.REQUIRED_FACT_NOT_FOUND,
                        "非 USD 付款缺少法币换算规则");
            }
            originalUsd = offRamp.payout().amount();
            quotedCrypto = offRamp.quotedCryptoAmount();
        } else {
            return RuleResult.pass(id());
        }

        if (!policy.evaluationTime().isAfter(expiresAt)) {
            return RuleResult.pass(id());
        }
        if (!(facts.referenceRate() instanceof LookupResult.Found)) {
            return RuleResult.fail(id(), Disposition.HOLD,
                    ReasonCode.REFERENCE_RATE_UNAVAILABLE,
                    "报价已过期且无法取得参考汇率");
        }
        LookupResult.Found<ReferenceRate> found =
                (LookupResult.Found<ReferenceRate>) facts.referenceRate();

        BigDecimal impliedRate = originalUsd.divide(quotedCrypto, MathContext.DECIMAL128);
        BigDecimal slippage = impliedRate.subtract(found.data().rate())
                .abs()
                .divide(found.data().rate(), MathContext.DECIMAL128);
        if (slippage.compareTo(policy.quoteSlippageTolerance()) > 0) {
            return RuleResult.fail(id(), Disposition.REQUOTE,
                    ReasonCode.QUOTE_SLIPPAGE_EXCEEDED,
                    "报价已过期，重算滑点 " + slippage.toPlainString()
                            + " 超过容忍值 " + policy.quoteSlippageTolerance());
        }
        return RuleResult.pass(id());
    }
}
