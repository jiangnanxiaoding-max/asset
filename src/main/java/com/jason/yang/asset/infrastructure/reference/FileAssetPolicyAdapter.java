package com.jason.yang.asset.infrastructure.reference;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jason.yang.asset.application.port.AssetPolicyPort;
import com.jason.yang.asset.domain.AssetNetwork;
import com.jason.yang.asset.domain.AssetNetworkPolicy;
import com.jason.yang.asset.domain.LookupResult;

import java.io.IOException;
import java.math.RoundingMode;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/** Asset/network policy adapter; scale defaults are explicit and documented in DECISIONS.md. */
public final class FileAssetPolicyAdapter implements AssetPolicyPort {
    private final Map<AssetNetwork, AssetNetworkPolicy> policies;

    public FileAssetPolicyAdapter(Path file, ObjectMapper mapper) {
        this.policies = load(file, mapper);
    }

    @Override
    public LookupResult<AssetNetworkPolicy> findPolicy(AssetNetwork assetNetwork) {
        AssetNetworkPolicy policy = policies.get(assetNetwork);
        return policy == null
                ? LookupResult.notFound("UNSUPPORTED_ASSET_OR_NETWORK")
                : LookupResult.found(policy, "assets.json:" + assetNetwork.asset() + "/" + assetNetwork.network());
    }

    private Map<AssetNetwork, AssetNetworkPolicy> load(Path file, ObjectMapper mapper) {
        try {
            JsonNode root = mapper.readTree(file.toFile());
            Map<AssetNetwork, AssetNetworkPolicy> loaded = new HashMap<>();
            root.fields().forEachRemaining(assetEntry ->
                    assetEntry.getValue().fields().forEachRemaining(networkEntry -> {
                        String asset = assetEntry.getKey();
                        String network = networkEntry.getKey();
                        JsonNode node = networkEntry.getValue();
                        int scale = 6;
                        if ("BTC".equalsIgnoreCase(asset)) scale = 8;
                        else if ("ETH".equalsIgnoreCase(asset)) scale = 18;
                        AssetNetwork key = new AssetNetwork(asset, network);
                        loaded.put(key, new AssetNetworkPolicy(
                                asset, network, node.path("min_amount").decimalValue(),
                                node.path("confirmations_required").asInt(), scale, RoundingMode.DOWN
                        ));
                    }));
            return java.util.Collections.unmodifiableMap(new HashMap<AssetNetwork, AssetNetworkPolicy>(loaded));
        } catch (IOException | RuntimeException exception) {
            throw new IllegalStateException("Cannot load asset reference file: " + file, exception);
        }
    }
}
