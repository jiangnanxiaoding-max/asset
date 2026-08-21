package com.jason.yang.asset.domain.policy;

import com.jason.yang.asset.domain.Disposition;
import com.jason.yang.asset.domain.DuplicateAssessment;
import com.jason.yang.asset.domain.InvestigationFacts;
import com.jason.yang.asset.domain.LookupResult;
import com.jason.yang.asset.domain.OffRampOrder;
import com.jason.yang.asset.domain.PolicySnapshot;
import com.jason.yang.asset.domain.ReasonCode;
import com.jason.yang.asset.domain.RuleResult;

public final class DuplicateFundsEventRule implements TriageRule {
    @Override
    public String id() {
        return "FUNDS_EVENT_IDEMPOTENCY";
    }

    @Override
    public RuleResult evaluate(InvestigationFacts facts, PolicySnapshot policy) {
        if (!(facts.order() instanceof OffRampOrder)) {
            return RuleResult.pass(id());
        }
        if (!(facts.duplicate() instanceof LookupResult.Found)) {
            if (facts.duplicate() instanceof LookupResult.NotFound<?>) {
                return RuleResult.fail(id(), Disposition.HOLD,
                        ReasonCode.REQUIRED_FACT_NOT_FOUND,
                        "无法确定资金事件是否已经处理");
            }
            return RuleResult.pass(id());
        }
        LookupResult.Found<DuplicateAssessment> found =
                (LookupResult.Found<DuplicateAssessment>) facts.duplicate();

        switch (found.data().status()) {
            case NEW:
                return RuleResult.pass(id());
            case ALREADY_CREDITED:
                return RuleResult.fail(
                    id(), Disposition.DUPLICATE_NOOP, ReasonCode.DUPLICATE_TX_HASH,
                    "交易已由订单 " + found.data().originalOrderId() + " 入账，不得重复处理");
            case IN_PROGRESS:
                return RuleResult.fail(
                    id(), Disposition.HOLD, ReasonCode.FUNDS_EVENT_IN_PROGRESS,
                    "相同资金事件正在处理中");
            case CONFLICT:
                return RuleResult.fail(
                    id(), Disposition.MANUAL_REVIEW, ReasonCode.FUNDS_EVENT_CONFLICT,
                    "资金事件登记信息存在冲突");
            default:
                throw new IllegalStateException("Unsupported duplicate status");
        }
    }
}
