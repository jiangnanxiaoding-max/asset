package com.jason.yang.asset.domain;

import java.util.List;

public final class TravelRuleAssessment {
    private final boolean required;
    private final boolean originatorComplete;
    private final boolean beneficiaryComplete;
    private final List<String> missingFields;

    public boolean required() {
        return required;
    }

    public boolean getRequired() {
        return required;
    }

    public boolean originatorComplete() {
        return originatorComplete;
    }

    public boolean getOriginatorComplete() {
        return originatorComplete;
    }

    public boolean beneficiaryComplete() {
        return beneficiaryComplete;
    }

    public boolean getBeneficiaryComplete() {
        return beneficiaryComplete;
    }

    public List<String> missingFields() {
        return missingFields;
    }

    public List<String> getMissingFields() {
        return missingFields;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof TravelRuleAssessment)) return false;
        TravelRuleAssessment that = (TravelRuleAssessment) other;
        return required == that.required
                && originatorComplete == that.originatorComplete
                && beneficiaryComplete == that.beneficiaryComplete
                && java.util.Objects.equals(missingFields, that.missingFields);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(required, originatorComplete, beneficiaryComplete, missingFields);
    }

    @Override
    public String toString() {
        return "TravelRuleAssessment{" + "required=" + required + ", originatorComplete=" + originatorComplete + ", beneficiaryComplete=" + beneficiaryComplete + ", missingFields=" + missingFields + "}";
    }


    public TravelRuleAssessment(boolean required, boolean originatorComplete, boolean beneficiaryComplete, List<String> missingFields) {
        missingFields = missingFields == null ? java.util.Collections.emptyList() : java.util.Collections.unmodifiableList(new java.util.ArrayList<>(missingFields));
    

        this.required = required;

        this.originatorComplete = originatorComplete;

        this.beneficiaryComplete = beneficiaryComplete;

        this.missingFields = missingFields;

    }

    public static TravelRuleAssessment notRequired() {
        return new TravelRuleAssessment(false, true, true, java.util.Collections.emptyList());
    }
}
