package com.jason.yang.asset.application.port;

import com.jason.yang.asset.domain.AssetNetworkPolicy;
import com.jason.yang.asset.domain.AssetNetwork;
import com.jason.yang.asset.domain.LookupResult;

public interface AssetPolicyPort {
    LookupResult<AssetNetworkPolicy> findPolicy(AssetNetwork assetNetwork);
}
