package com.jason.yang.asset.application.port;

import com.jason.yang.asset.domain.LookupResult;
import com.jason.yang.asset.domain.ReferenceRate;
import com.jason.yang.asset.domain.AssetNetwork;

import java.time.Instant;

public interface ReferenceRatePort {
    LookupResult<ReferenceRate> getRate(AssetNetwork assetNetwork, String quoteCurrency, Instant asOf);
}
