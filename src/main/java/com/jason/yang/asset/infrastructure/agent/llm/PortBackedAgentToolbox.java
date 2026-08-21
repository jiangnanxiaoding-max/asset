package com.jason.yang.asset.infrastructure.agent.llm;

import com.jason.yang.asset.application.port.AddressRiskPort;
import com.jason.yang.asset.application.port.AssetPolicyPort;
import com.jason.yang.asset.application.port.BlockchainDepositPort;
import com.jason.yang.asset.application.port.CustomerProfilePort;
import com.jason.yang.asset.application.port.FiatReceiptPort;
import com.jason.yang.asset.application.port.FundsEventRegistryPort;
import com.jason.yang.asset.application.port.ReferenceRatePort;
import com.jason.yang.asset.application.port.TravelRulePort;
import com.jason.yang.asset.application.port.WalletFundsPort;
import com.jason.yang.asset.domain.BlockchainAddress;
import com.jason.yang.asset.domain.CounterpartyInfo;
import com.jason.yang.asset.domain.FundingEvidence;
import com.jason.yang.asset.domain.LookupResult;
import com.jason.yang.asset.domain.OffRampOrder;
import com.jason.yang.asset.domain.OnRampOrder;
import com.jason.yang.asset.domain.Order;
import com.jason.yang.asset.domain.ReferenceRate;
import com.jason.yang.asset.domain.TravelRuleAssessment;
import com.jason.yang.asset.domain.WithdrawalOrder;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Adapts existing authoritative query ports into a fixed allow-list of LLM tools. */
public class PortBackedAgentToolbox implements AgentToolbox {
    public static final String CUSTOMER_PROFILE = "customer_profile";
    public static final String ASSET_POLICY = "asset_policy";
    public static final String ADDRESS_RISK = "address_risk";
    public static final String FUNDING = "funding";
    public static final String REFERENCE_RATE = "reference_rate";
    public static final String TRAVEL_RULE = "travel_rule";
    public static final String DUPLICATE_FUNDS_EVENT = "duplicate_funds_event";

    private final CustomerProfilePort customerPort;
    private final AssetPolicyPort assetPolicyPort;
    private final AddressRiskPort addressRiskPort;
    private final FiatReceiptPort fiatReceiptPort;
    private final BlockchainDepositPort blockchainDepositPort;
    private final WalletFundsPort walletFundsPort;
    private final ReferenceRatePort referenceRatePort;
    private final TravelRulePort travelRulePort;
    private final FundsEventRegistryPort fundsEventRegistryPort;
    private final Map<String, AgentToolDefinition> tools;

    public PortBackedAgentToolbox(
            CustomerProfilePort customerPort,
            AssetPolicyPort assetPolicyPort,
            AddressRiskPort addressRiskPort,
            FiatReceiptPort fiatReceiptPort,
            BlockchainDepositPort blockchainDepositPort,
            WalletFundsPort walletFundsPort,
            ReferenceRatePort referenceRatePort,
            TravelRulePort travelRulePort,
            FundsEventRegistryPort fundsEventRegistryPort
    ) {
        this.customerPort = Objects.requireNonNull(customerPort);
        this.assetPolicyPort = Objects.requireNonNull(assetPolicyPort);
        this.addressRiskPort = Objects.requireNonNull(addressRiskPort);
        this.fiatReceiptPort = Objects.requireNonNull(fiatReceiptPort);
        this.blockchainDepositPort = Objects.requireNonNull(blockchainDepositPort);
        this.walletFundsPort = Objects.requireNonNull(walletFundsPort);
        this.referenceRatePort = Objects.requireNonNull(referenceRatePort);
        this.travelRulePort = Objects.requireNonNull(travelRulePort);
        this.fundsEventRegistryPort = Objects.requireNonNull(fundsEventRegistryPort);

        java.util.List<String> allOrders = java.util.Arrays.asList(
                "OnRampOrder", "OffRampOrder", "WithdrawalOrder");
        Map<String, AgentToolDefinition> configuredTools =
                new LinkedHashMap<String, AgentToolDefinition>();
        configuredTools.put(CUSTOMER_PROFILE, definition(CUSTOMER_PROFILE,
                "Query KYC status, customer status and transaction limits for the current order",
                FactType.CUSTOMER, allOrders, Collections.<FactType>emptyList()));
        configuredTools.put(ASSET_POLICY, definition(ASSET_POLICY,
                "Query whether the current asset and network are supported",
                FactType.ASSET_POLICY, allOrders, Collections.<FactType>emptyList()));
        configuredTools.put(ADDRESS_RISK, definition(ADDRESS_RISK,
                "Screen the order-bound blockchain address for sanctions and risk",
                FactType.ADDRESS_RISK, allOrders, Collections.<FactType>emptyList()));
        configuredTools.put(FUNDING, definition(FUNDING,
                "Query authoritative funding, deposit or wallet reservation evidence",
                FactType.FUNDING, allOrders, Collections.<FactType>emptyList()));
        configuredTools.put(REFERENCE_RATE, definition(REFERENCE_RATE,
                "Query the required reference FX or crypto price for the current order",
                FactType.REFERENCE_RATE, allOrders, Collections.<FactType>emptyList()));
        configuredTools.put(TRAVEL_RULE, definition(TRAVEL_RULE,
                "Assess Travel Rule applicability and required counterparty information",
                FactType.TRAVEL_RULE, allOrders,
                Collections.singletonList(FactType.REFERENCE_RATE)));
        configuredTools.put(DUPLICATE_FUNDS_EVENT, definition(DUPLICATE_FUNDS_EVENT,
                "Check whether the off-ramp deposit event has already been credited",
                FactType.DUPLICATE, Collections.singletonList("OffRampOrder"),
                Collections.<FactType>emptyList()));
        this.tools = Collections.unmodifiableMap(configuredTools);
    }

    @Override
    public Map<String, AgentToolDefinition> tools() {
        return tools;
    }

    private AgentToolDefinition definition(
            String name,
            String description,
            FactType factType,
            java.util.List<String> orderTypes,
            java.util.List<FactType> prerequisites
    ) {
        return new AgentToolDefinition(name, description, factType, orderTypes, prerequisites);
    }

    /** Executes one server-bound query; the model cannot supply customer IDs, addresses or amounts. */
    @Override
    public ToolResult invoke(String toolName, ToolContext context) {
        Objects.requireNonNull(toolName);
        Objects.requireNonNull(context);
        Order order = context.order();

        if (CUSTOMER_PROFILE.equals(toolName)) {
            return new ToolResult(FactType.CUSTOMER,
                    customerPort.findCustomer(order.customerIdentity()));
        }
        if (ASSET_POLICY.equals(toolName)) {
            return new ToolResult(FactType.ASSET_POLICY,
                    assetPolicyPort.findPolicy(order.assetNetworkIdentity()));
        }
        if (ADDRESS_RISK.equals(toolName)) {
            String network = order.network();
            if (order instanceof OffRampOrder) {
                network = ((OffRampOrder) order).deposit().observedNetwork();
            }
            return new ToolResult(FactType.ADDRESS_RISK,
                    addressRiskPort.screen(new BlockchainAddress(order.screenedAddress(), network)));
        }
        if (FUNDING.equals(toolName)) {
            return new ToolResult(FactType.FUNDING, funding(order));
        }
        if (REFERENCE_RATE.equals(toolName)) {
            return new ToolResult(FactType.REFERENCE_RATE,
                    referenceRate(order, context));
        }
        if (TRAVEL_RULE.equals(toolName)) {
            return new ToolResult(FactType.TRAVEL_RULE,
                    travelRule(order, context));
        }
        if (DUPLICATE_FUNDS_EVENT.equals(toolName)) {
            return new ToolResult(FactType.DUPLICATE, duplicate(order));
        }
        throw new IllegalArgumentException("Tool is not allow-listed: " + toolName);
    }

    private LookupResult<FundingEvidence> funding(Order order) {
        if (order instanceof OnRampOrder) {
            return fiatReceiptPort.getReceipt((OnRampOrder) order);
        }
        if (order instanceof OffRampOrder) {
            return blockchainDepositPort.getDeposit((OffRampOrder) order);
        }
        return walletFundsPort.getAvailableAndReservedFunds((WithdrawalOrder) order);
    }

    private LookupResult<?> duplicate(Order order) {
        if (order instanceof OffRampOrder) {
            return fundsEventRegistryPort.assess((OffRampOrder) order);
        }
        return LookupResult.notApplicable();
    }

    private LookupResult<ReferenceRate> referenceRate(Order order, ToolContext context) {
        boolean required = order instanceof WithdrawalOrder;
        if (order instanceof OnRampOrder) {
            required = context.policy().evaluationTime()
                    .isAfter(((OnRampOrder) order).quoteExpiresAt());
        } else if (order instanceof OffRampOrder) {
            required = context.policy().evaluationTime()
                    .isAfter(((OffRampOrder) order).quoteExpiresAt());
        }
        if (!required) {
            return LookupResult.notApplicable();
        }
        return referenceRatePort.getRate(
                order.assetNetworkIdentity(), "USD", context.policy().evaluationTime());
    }

    private LookupResult<TravelRuleAssessment> travelRule(Order order, ToolContext context) {
        if (order.counterparty().vaspStatus() == CounterpartyInfo.VaspStatus.NOT_VASP) {
            return LookupResult.found(
                    TravelRuleAssessment.notRequired(), "counterparty:not-vasp");
        }
        if (order.counterparty().vaspStatus() == CounterpartyInfo.VaspStatus.UNKNOWN) {
            return LookupResult.notFound("VASP_STATUS_UNKNOWN");
        }

        BigDecimal usdEquivalent = usdEquivalent(order, context);
        if (usdEquivalent == null) {
            return LookupResult.unavailable("USD_EQUIVALENT_UNAVAILABLE", false);
        }
        return travelRulePort.assess(order, usdEquivalent);
    }

    private BigDecimal usdEquivalent(Order order, ToolContext context) {
        if (order instanceof OnRampOrder) {
            return ((OnRampOrder) order).fiatAmountUsd();
        }
        if (order instanceof OffRampOrder) {
            OffRampOrder offRamp = (OffRampOrder) order;
            return "USD".equalsIgnoreCase(offRamp.payout().currency())
                    ? offRamp.payout().amount() : null;
        }
        if (!context.referenceRate().isPresent()) {
            return null;
        }
        LookupResult<ReferenceRate> rate = context.referenceRate().get();
        if (!rate.value().isPresent()) {
            return null;
        }
        return ((WithdrawalOrder) order).amount().multiply(rate.value().get().rate());
    }
}
