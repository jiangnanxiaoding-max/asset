package com.jason.yang.asset.infrastructure.funding;

import com.jason.yang.asset.application.port.BlockchainDepositPort;
import com.jason.yang.asset.domain.FundingEvidence;
import com.jason.yang.asset.domain.DepositReference;
import com.jason.yang.asset.domain.LookupResult;
import com.jason.yang.asset.domain.OffRampOrder;

/** Offline-only adapter translating embedded deposit observations into authoritative-looking test facts. */
public final class EmbeddedBlockchainDepositStubAdapter implements BlockchainDepositPort {
    @Override
    public LookupResult<FundingEvidence> getDeposit(OffRampOrder order) {
        DepositReference deposit = order.deposit();
        return LookupResult.found(new FundingEvidence.Chain(
                deposit.embeddedStatus(), deposit.eventKey(), deposit.observedNetwork(),
                deposit.observedAmount(), deposit.confirmations()
        ), "embedded-deposit-stub:" + deposit.eventKey());
    }
}
