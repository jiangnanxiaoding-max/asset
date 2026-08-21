package com.jason.yang.asset.domain.policy;


import java.util.List;

public final class CoreRules {
    private CoreRules() {}

    public static List<TriageRule> standard() {
        return java.util.Arrays.asList(
                new FactAvailabilityRule(),
                new AddressRiskRule(),
                new CustomerRule(),
                new AssetNetworkRule(),
                new FundingRule(),
                new AmountMatchRule(),
                new QuoteRule(),
                new BankAccountRule(),
                new TravelRule(),
                new DuplicateFundsEventRule()
        );
    }
}
