package com.jason.yang.asset.infrastructure.funding;

import com.jason.yang.asset.application.port.WalletFundsPort;
import com.jason.yang.asset.domain.FundingEvidence;
import com.jason.yang.asset.domain.LookupResult;
import com.jason.yang.asset.domain.WithdrawalOrder;

/** Fail-closed placeholder because the exercise has no wallet ledger or reservation source. */
public final class UnavailableWalletFundsAdapter implements WalletFundsPort {
    @Override
    public LookupResult<FundingEvidence> getAvailableAndReservedFunds(WithdrawalOrder order) {
        return LookupResult.unavailable("WALLET_LEDGER_NOT_PROVIDED", false);
    }
}
