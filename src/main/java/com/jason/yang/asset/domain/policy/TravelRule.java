package com.jason.yang.asset.domain.policy;

import com.jason.yang.asset.domain.Disposition;
import com.jason.yang.asset.domain.InvestigationFacts;
import com.jason.yang.asset.domain.LookupResult;
import com.jason.yang.asset.domain.PolicySnapshot;
import com.jason.yang.asset.domain.ReasonCode;
import com.jason.yang.asset.domain.RuleResult;
import com.jason.yang.asset.domain.TravelRuleAssessment;

public final class TravelRule implements TriageRule {
    @Override
    public String id() {
        return "TRAVEL_RULE";
    }

    @Override
    public RuleResult evaluate(InvestigationFacts facts, PolicySnapshot policy) {
        if (facts.order().counterparty().vaspStatus()
                == com.jason.yang.asset.domain.CounterpartyInfo.VaspStatus.UNKNOWN) {
            return RuleResult.fail(id(), Disposition.HOLD,
                    ReasonCode.VASP_STATUS_UNKNOWN,
                    "无法确认交易对手是否为 VASP，需要人工核实");
        }
        if (!(facts.travelRule() instanceof LookupResult.Found)) {
            if (facts.travelRule() instanceof LookupResult.NotFound<?>) {
                return RuleResult.fail(id(), Disposition.HOLD,
                        ReasonCode.TRAVEL_RULE_INFO_MISSING,
                        "Travel Rule 评估信息缺失");
            }
            return RuleResult.pass(id());
        }
        LookupResult.Found<TravelRuleAssessment> found =
                (LookupResult.Found<TravelRuleAssessment>) facts.travelRule();

        TravelRuleAssessment assessment = found.data();
        if (assessment.required()
                && (!assessment.originatorComplete() || !assessment.beneficiaryComplete())) {
            return RuleResult.fail(id(), Disposition.HOLD,
                    ReasonCode.TRAVEL_RULE_INFO_MISSING,
                    "达到 Travel Rule 阈值但缺少信息："
                            + String.join(",", assessment.missingFields()));
        }
        return RuleResult.pass(id());
    }
}
