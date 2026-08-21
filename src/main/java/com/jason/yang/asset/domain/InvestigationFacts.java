package com.jason.yang.asset.domain;

import java.util.Objects;

public final class InvestigationFacts {
    private final Order order;
    private final LookupResult<CustomerProfile> customer;
    private final LookupResult<AssetNetworkPolicy> assetPolicy;
    private final LookupResult<AddressRiskAssessment> addressRisk;
    private final LookupResult<FundingEvidence> funding;
    private final LookupResult<ReferenceRate> referenceRate;
    private final LookupResult<TravelRuleAssessment> travelRule;
    private final LookupResult<DuplicateAssessment> duplicate;

    public Order order() {
        return order;
    }

    public Order getOrder() {
        return order;
    }

    public LookupResult<CustomerProfile> customer() {
        return customer;
    }

    public LookupResult<CustomerProfile> getCustomer() {
        return customer;
    }

    public LookupResult<AssetNetworkPolicy> assetPolicy() {
        return assetPolicy;
    }

    public LookupResult<AssetNetworkPolicy> getAssetPolicy() {
        return assetPolicy;
    }

    public LookupResult<AddressRiskAssessment> addressRisk() {
        return addressRisk;
    }

    public LookupResult<AddressRiskAssessment> getAddressRisk() {
        return addressRisk;
    }

    public LookupResult<FundingEvidence> funding() {
        return funding;
    }

    public LookupResult<FundingEvidence> getFunding() {
        return funding;
    }

    public LookupResult<ReferenceRate> referenceRate() {
        return referenceRate;
    }

    public LookupResult<ReferenceRate> getReferenceRate() {
        return referenceRate;
    }

    public LookupResult<TravelRuleAssessment> travelRule() {
        return travelRule;
    }

    public LookupResult<TravelRuleAssessment> getTravelRule() {
        return travelRule;
    }

    public LookupResult<DuplicateAssessment> duplicate() {
        return duplicate;
    }

    public LookupResult<DuplicateAssessment> getDuplicate() {
        return duplicate;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof InvestigationFacts)) return false;
        InvestigationFacts that = (InvestigationFacts) other;
        return java.util.Objects.equals(order, that.order)
                && java.util.Objects.equals(customer, that.customer)
                && java.util.Objects.equals(assetPolicy, that.assetPolicy)
                && java.util.Objects.equals(addressRisk, that.addressRisk)
                && java.util.Objects.equals(funding, that.funding)
                && java.util.Objects.equals(referenceRate, that.referenceRate)
                && java.util.Objects.equals(travelRule, that.travelRule)
                && java.util.Objects.equals(duplicate, that.duplicate);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(order, customer, assetPolicy, addressRisk, funding, referenceRate, travelRule, duplicate);
    }

    @Override
    public String toString() {
        return "InvestigationFacts{" + "order=" + order + ", customer=" + customer + ", assetPolicy=" + assetPolicy + ", addressRisk=" + addressRisk + ", funding=" + funding + ", referenceRate=" + referenceRate + ", travelRule=" + travelRule + ", duplicate=" + duplicate + "}";
    }


    public InvestigationFacts(Order order, LookupResult<CustomerProfile> customer, LookupResult<AssetNetworkPolicy> assetPolicy, LookupResult<AddressRiskAssessment> addressRisk, LookupResult<FundingEvidence> funding, LookupResult<ReferenceRate> referenceRate, LookupResult<TravelRuleAssessment> travelRule, LookupResult<DuplicateAssessment> duplicate) {
        Objects.requireNonNull(order);
        Objects.requireNonNull(customer);
        Objects.requireNonNull(assetPolicy);
        Objects.requireNonNull(addressRisk);
        Objects.requireNonNull(funding);
        Objects.requireNonNull(referenceRate);
        Objects.requireNonNull(travelRule);
        Objects.requireNonNull(duplicate);
    

        this.order = order;

        this.customer = customer;

        this.assetPolicy = assetPolicy;

        this.addressRisk = addressRisk;

        this.funding = funding;

        this.referenceRate = referenceRate;

        this.travelRule = travelRule;

        this.duplicate = duplicate;

    }
}
