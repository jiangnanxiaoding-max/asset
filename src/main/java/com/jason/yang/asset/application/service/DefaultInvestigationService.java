package com.jason.yang.asset.application.service;

import com.jason.yang.asset.application.port.AddressRiskPort;
import com.jason.yang.asset.application.port.AssetPolicyPort;
import com.jason.yang.asset.application.port.BlockchainDepositPort;
import com.jason.yang.asset.application.port.CustomerProfilePort;
import com.jason.yang.asset.application.port.FiatReceiptPort;
import com.jason.yang.asset.application.port.FundsEventRegistryPort;
import com.jason.yang.asset.application.port.InvestigationPort;
import com.jason.yang.asset.application.port.ReferenceRatePort;
import com.jason.yang.asset.application.port.TravelRulePort;
import com.jason.yang.asset.application.port.WalletFundsPort;
import com.jason.yang.asset.domain.CounterpartyInfo;
import com.jason.yang.asset.domain.CustomerProfile;
import com.jason.yang.asset.domain.AssetNetworkPolicy;
import com.jason.yang.asset.domain.AddressRiskAssessment;
import com.jason.yang.asset.domain.BlockchainAddress;
import com.jason.yang.asset.domain.DuplicateAssessment;
import com.jason.yang.asset.domain.FundingEvidence;
import com.jason.yang.asset.domain.InvestigationFacts;
import com.jason.yang.asset.domain.LookupResult;
import com.jason.yang.asset.domain.OffRampOrder;
import com.jason.yang.asset.domain.OnRampOrder;
import com.jason.yang.asset.domain.Order;
import com.jason.yang.asset.domain.PolicySnapshot;
import com.jason.yang.asset.domain.ReferenceRate;
import com.jason.yang.asset.domain.TravelRuleAssessment;
import com.jason.yang.asset.domain.WithdrawalOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.Objects;

/** Collects authoritative facts without making any business disposition. */
public final class DefaultInvestigationService implements InvestigationPort {
    private static final Logger log = LoggerFactory.getLogger(DefaultInvestigationService.class);

    private final CustomerProfilePort customerPort;
    private final AssetPolicyPort assetPolicyPort;
    private final AddressRiskPort addressRiskPort;
    private final FiatReceiptPort fiatReceiptPort;
    private final BlockchainDepositPort blockchainDepositPort;
    private final WalletFundsPort walletFundsPort;
    private final ReferenceRatePort referenceRatePort;
    private final TravelRulePort travelRulePort;
    private final FundsEventRegistryPort fundsEventRegistryPort;

    public DefaultInvestigationService(
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
    }

    /** Selects funding and screening tools by order type and returns an immutable fact snapshot. */
    @Override
    public InvestigationFacts investigate(Order order, PolicySnapshot policy) {
        log.info("investigation started orderId={} orderType={}",
                order.identity().value(), order.getClass().getSimpleName());
        try {
            LookupResult<CustomerProfile> customer = customerPort.findCustomer(order.customerIdentity());
            LookupResult<AssetNetworkPolicy> assetPolicy = assetPolicyPort.findPolicy(order.assetNetworkIdentity());
            String screeningNetwork = order.network();
            if (order instanceof OffRampOrder) {
                screeningNetwork = ((OffRampOrder) order).deposit().observedNetwork();
            }
            LookupResult<AddressRiskAssessment> addressRisk = addressRiskPort.screen(
                    new BlockchainAddress(order.screenedAddress(), screeningNetwork)
            );
            LookupResult<FundingEvidence> funding = funding(order);
            LookupResult<DuplicateAssessment> duplicate = duplicate(order);
            LookupResult<ReferenceRate> rate = referenceRate(order, policy);
            LookupResult<TravelRuleAssessment> travelRule = travelRule(order, rate);

            InvestigationFacts facts = new InvestigationFacts(
                    order,
                    customer,
                    assetPolicy,
                    addressRisk,
                    funding,
                    rate,
                    travelRule,
                    duplicate
            );
            long unavailableCount = java.util.stream.Stream.of(
                            customer, assetPolicy, addressRisk, funding, rate, travelRule, duplicate
                    )
                    .filter(result -> result instanceof LookupResult.Unavailable<?>
                            || result instanceof LookupResult.Conflict<?>)
                    .count();
            if (unavailableCount > 0) {
                log.warn("investigation completed with unavailable facts orderId={} unavailableCount={}",
                        order.identity().value(), unavailableCount);
            } else {
                log.info("investigation completed orderId={}", order.identity().value());
            }
            return facts;
        } catch (RuntimeException exception) {
            log.error("investigation failed orderId={}", order.identity().value(), exception);
            throw exception;
        }
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

    private LookupResult<DuplicateAssessment> duplicate(Order order) {
        if (order instanceof OffRampOrder) {
            return fundsEventRegistryPort.assess((OffRampOrder) order);
        }
        return LookupResult.notApplicable();
    }

    private LookupResult<ReferenceRate> referenceRate(Order order, PolicySnapshot policy) {
        boolean required = order instanceof WithdrawalOrder;
        if (order instanceof OnRampOrder) {
            required = policy.evaluationTime().isAfter(((OnRampOrder) order).quoteExpiresAt());
        } else if (order instanceof OffRampOrder) {
            required = policy.evaluationTime().isAfter(((OffRampOrder) order).quoteExpiresAt());
        }

        if (!required) {
            return LookupResult.notApplicable();
        }
        return referenceRatePort.getRate(order.assetNetworkIdentity(), "USD", policy.evaluationTime());
    }

    private LookupResult<TravelRuleAssessment> travelRule(
            Order order,
            LookupResult<ReferenceRate> rate
    ) {
        if (order.counterparty().vaspStatus() == CounterpartyInfo.VaspStatus.NOT_VASP) {
            return LookupResult.found(TravelRuleAssessment.notRequired(), "counterparty:not-vasp");
        }
        if (order.counterparty().vaspStatus() == CounterpartyInfo.VaspStatus.UNKNOWN) {
            return LookupResult.notFound("VASP_STATUS_UNKNOWN");
        }

        BigDecimal usdEquivalent = usdEquivalent(order, rate);
        if (usdEquivalent == null) {
            return LookupResult.unavailable("USD_EQUIVALENT_UNAVAILABLE", false);
        }
        return travelRulePort.assess(order, usdEquivalent);
    }

    private BigDecimal usdEquivalent(Order order, LookupResult<ReferenceRate> rate) {
        if (order instanceof OnRampOrder) {
            return ((OnRampOrder) order).fiatAmountUsd();
        }
        if (order instanceof OffRampOrder) {
            OffRampOrder offRamp = (OffRampOrder) order;
            return "USD".equalsIgnoreCase(offRamp.payout().currency())
                    ? offRamp.payout().amount() : null;
        }
        final WithdrawalOrder withdrawal = (WithdrawalOrder) order;
        return rate.value().map(value -> withdrawal.amount().multiply(value.rate())).orElse(null);
    }
}
