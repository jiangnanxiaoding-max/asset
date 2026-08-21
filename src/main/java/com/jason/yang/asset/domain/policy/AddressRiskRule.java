package com.jason.yang.asset.domain.policy;

import com.jason.yang.asset.domain.AddressRiskAssessment;
import com.jason.yang.asset.domain.Disposition;
import com.jason.yang.asset.domain.InvestigationFacts;
import com.jason.yang.asset.domain.LookupResult;
import com.jason.yang.asset.domain.PolicySnapshot;
import com.jason.yang.asset.domain.ReasonCode;
import com.jason.yang.asset.domain.RuleResult;

public final class AddressRiskRule implements TriageRule {
    @Override
    public String id() {
        return "ADDRESS_RISK";
    }

    @Override
    public RuleResult evaluate(InvestigationFacts facts, PolicySnapshot policy) {
        if (facts.addressRisk() instanceof LookupResult.Found) {
            LookupResult.Found<AddressRiskAssessment> found =
                    (LookupResult.Found<AddressRiskAssessment>) facts.addressRisk();
            AddressRiskAssessment risk = found.data();
            if (risk.category() == AddressRiskAssessment.RiskCategory.SANCTIONED) {
                return RuleResult.fail(id(), Disposition.FREEZE_COMPLIANCE,
                        ReasonCode.ADDRESS_SANCTIONED,
                        "对手方地址命中 sanctioned 类别");
            }
            if (risk.riskScore() >= 90) {
                return RuleResult.fail(id(), Disposition.FREEZE_COMPLIANCE,
                        ReasonCode.ADDRESS_HIGH_RISK,
                        "对手方地址风险分为 " + risk.riskScore() + "，达到冻结阈值");
            }
            if (risk.category() == AddressRiskAssessment.RiskCategory.MIXER
                    || risk.category() == AddressRiskAssessment.RiskCategory.DARKNET
                    || risk.riskScore() >= 70) {
                return RuleResult.fail(id(), Disposition.HOLD,
                        ReasonCode.ADDRESS_MEDIUM_RISK,
                        "对手方地址类别为 " + risk.category()
                                + "，风险分为 " + risk.riskScore() + "，需合规复核");
            }
            if (risk.category() == AddressRiskAssessment.RiskCategory.UNKNOWN) {
                return unknown();
            }
            return RuleResult.pass(id());
        }

        if (facts.addressRisk() instanceof LookupResult.NotFound<?>) {
            return unknown();
        }
        return RuleResult.pass(id());
    }

    private RuleResult unknown() {
        return RuleResult.fail(id(), Disposition.HOLD,
                ReasonCode.ADDRESS_UNKNOWN,
                "地址风险信息缺失或类别未知，需人工复核");
    }
}
