package com.jason.yang.asset.application.port;

import com.jason.yang.asset.domain.FundingEvidence;
import com.jason.yang.asset.domain.LookupResult;
import com.jason.yang.asset.domain.OffRampOrder;

public interface BlockchainDepositPort {
    LookupResult<FundingEvidence> getDeposit(OffRampOrder order);
}
