package com.jason.yang.asset.domain.policy;

import com.jason.yang.asset.domain.Disposition;
import com.jason.yang.asset.domain.InvestigationFacts;
import com.jason.yang.asset.domain.LookupResult;
import com.jason.yang.asset.domain.PolicySnapshot;
import com.jason.yang.asset.domain.ReasonCode;
import com.jason.yang.asset.domain.RuleResult;

import java.util.List;

public final class FactAvailabilityRule implements TriageRule {
    @Override
    public String id() {
        return "FACT_AVAILABILITY";
    }

    @Override
    public RuleResult evaluate(InvestigationFacts facts, PolicySnapshot policy) {
        List<LookupResult<?>> results = java.util.Arrays.asList(
                facts.customer(),
                facts.assetPolicy(),
                facts.addressRisk(),
                facts.funding(),
                facts.referenceRate(),
                facts.travelRule(),
                facts.duplicate()
        );

        /**
         * 外部数据冲突
         */
        if (results.stream().anyMatch(result -> result instanceof LookupResult.Conflict<?>)) {
            return RuleResult.fail(
                    id(),
                    Disposition.MANUAL_REVIEW,
                    ReasonCode.DATA_CONFLICT,
                    "权威事实存在冲突，已停止自动处理"
            );
        }
        /**
         * 事实服务不可用
         */
        if (results.stream().anyMatch(result -> result instanceof LookupResult.Unavailable<?>)) {
            return RuleResult.fail(
                    id(),
                    Disposition.HOLD,
                    ReasonCode.TOOL_UNAVAILABLE,
                    "必要查询不可用或已耗尽重试预算"
            );
        }
        return RuleResult.pass(id());
    }
}
