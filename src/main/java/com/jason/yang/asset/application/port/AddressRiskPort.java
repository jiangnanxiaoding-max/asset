package com.jason.yang.asset.application.port;

import com.jason.yang.asset.domain.AddressRiskAssessment;
import com.jason.yang.asset.domain.BlockchainAddress;
import com.jason.yang.asset.domain.LookupResult;

public interface AddressRiskPort {
    LookupResult<AddressRiskAssessment> screen(BlockchainAddress address);
}
