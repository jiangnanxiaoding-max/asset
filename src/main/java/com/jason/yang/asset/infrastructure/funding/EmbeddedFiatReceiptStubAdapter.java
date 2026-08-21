package com.jason.yang.asset.infrastructure.funding;

import com.jason.yang.asset.application.port.FiatReceiptPort;
import com.jason.yang.asset.domain.FundingEvidence;
import com.jason.yang.asset.domain.LookupResult;
import com.jason.yang.asset.domain.OnRampOrder;

/** Offline-only adapter translating the embedded fiat status into a funding fact. */
public final class EmbeddedFiatReceiptStubAdapter implements FiatReceiptPort {
    @Override
    public LookupResult<FundingEvidence> getReceipt(OnRampOrder order) {
        return LookupResult.found(
                new FundingEvidence.Fiat(order.embeddedFiatStatus(), order.fiatAmountUsd()),
                "embedded-fiat-stub:" + order.orderId()
        );
    }
}
