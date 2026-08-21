package com.jason.yang.asset.domain.policy;

import com.jason.yang.asset.domain.CustomerProfile;
import com.jason.yang.asset.domain.Disposition;
import com.jason.yang.asset.domain.InvestigationFacts;
import com.jason.yang.asset.domain.LookupResult;
import com.jason.yang.asset.domain.OffRampOrder;
import com.jason.yang.asset.domain.OnRampOrder;
import com.jason.yang.asset.domain.PolicySnapshot;
import com.jason.yang.asset.domain.ReasonCode;
import com.jason.yang.asset.domain.ReferenceRate;
import com.jason.yang.asset.domain.RuleResult;
import com.jason.yang.asset.domain.WithdrawalOrder;

import java.math.BigDecimal;

public final class CustomerRule implements TriageRule {
    @Override
    public String id() {
        return "CUSTOMER_STATUS_AND_LIMIT";
    }

    @Override
    public RuleResult evaluate(InvestigationFacts facts, PolicySnapshot policy) {
        if (!(facts.customer() instanceof LookupResult.Found)) {
            if (facts.customer() instanceof LookupResult.NotFound<?>) {
                return RuleResult.fail(id(), Disposition.MANUAL_REVIEW,
                        ReasonCode.CUSTOMER_NOT_FOUND,
                        "权威客户源中不存在该客户");
            }
            return RuleResult.pass(id());
        }
        LookupResult.Found<CustomerProfile> found =
                (LookupResult.Found<CustomerProfile>) facts.customer();

        CustomerProfile customer = found.data();
        if (customer.status() != CustomerProfile.Status.ACTIVE) {
            return RuleResult.fail(id(), Disposition.REJECT_ESCALATE,
                    ReasonCode.CUSTOMER_NOT_ACTIVE,
                    "客户状态为 " + customer.status() + "，不允许自动处理");
        }

        BigDecimal orderUsd = orderUsd(facts);
        if (orderUsd == null) {
            return RuleResult.fail(id(), Disposition.HOLD,
                    ReasonCode.REQUIRED_FACT_NOT_FOUND,
                    "无法取得订单美元等值，不能完成限额检查");
        }
        if (orderUsd.compareTo(customer.monthlyLimitUsd()) > 0) {
            return RuleResult.fail(id(), Disposition.REJECT_ESCALATE,
                    ReasonCode.LIMIT_EXCEEDED,
                    "订单美元等值 " + orderUsd + " 超过客户限额 " + customer.monthlyLimitUsd());
        }
        return RuleResult.pass(id());
    }

    private BigDecimal orderUsd(InvestigationFacts facts) {
        if (facts.order() instanceof OnRampOrder) {
            return ((OnRampOrder) facts.order()).fiatAmountUsd();
        }
        if (facts.order() instanceof OffRampOrder) {
            OffRampOrder offRamp = (OffRampOrder) facts.order();
            return "USD".equalsIgnoreCase(offRamp.payout().currency())
                    ? offRamp.payout().amount() : null;
        }
        final WithdrawalOrder withdrawal = (WithdrawalOrder) facts.order();
        return facts.referenceRate().value().map(ReferenceRate::rate)
                .map(withdrawal.amount()::multiply).orElse(null);
    }
}
