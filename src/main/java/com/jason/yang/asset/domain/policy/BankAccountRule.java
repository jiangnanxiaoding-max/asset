package com.jason.yang.asset.domain.policy;

import com.jason.yang.asset.domain.CustomerProfile;
import com.jason.yang.asset.domain.Disposition;
import com.jason.yang.asset.domain.InvestigationFacts;
import com.jason.yang.asset.domain.LookupResult;
import com.jason.yang.asset.domain.OffRampOrder;
import com.jason.yang.asset.domain.PolicySnapshot;
import com.jason.yang.asset.domain.ReasonCode;
import com.jason.yang.asset.domain.RuleResult;

public final class BankAccountRule implements TriageRule {
    @Override
    public String id() {
        return "BANK_ACCOUNT_OWNER";
    }

    @Override
    public RuleResult evaluate(InvestigationFacts facts, PolicySnapshot policy) {
        if (!(facts.order() instanceof OffRampOrder)) {
            return RuleResult.pass(id());
        }
        OffRampOrder offRamp = (OffRampOrder) facts.order();
        if (!(facts.customer() instanceof LookupResult.Found)) {
            return RuleResult.pass(id());
        }
        LookupResult.Found<CustomerProfile> customer =
                (LookupResult.Found<CustomerProfile>) facts.customer();

        String actual = normalize(offRamp.payout().bankAccountName());
        String expected = normalize(customer.data().verifiedBankName());
        if (!actual.equals(expected)) {
            return RuleResult.fail(id(), Disposition.REJECT_ESCALATE,
                    ReasonCode.BANK_NAME_MISMATCH,
                    "银行账户名与客户已验证户名不一致");
        }
        return RuleResult.pass(id());
    }

    private String normalize(String name) {
        return name.trim().replaceAll("\\s+", " ");
    }
}
