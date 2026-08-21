package com.jason.yang.asset.infrastructure.reference;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jason.yang.asset.application.port.ReferenceRatePort;
import com.jason.yang.asset.domain.AssetNetwork;
import com.jason.yang.asset.domain.LookupResult;
import com.jason.yang.asset.domain.ReferenceRate;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/** Reference-rate snapshot adapter. */
public final class FileReferenceRateAdapter implements ReferenceRatePort {
    private final Map<String, java.math.BigDecimal> rates;

    public FileReferenceRateAdapter(Path file, ObjectMapper mapper) {
        this.rates = load(file, mapper);
    }

    @Override
    public LookupResult<ReferenceRate> getRate(
            AssetNetwork assetNetwork, String quoteCurrency, Instant asOf
    ) {
        String pair = assetNetwork.asset() + "/" + quoteCurrency.toUpperCase();
        BigDecimal value = rates.get(pair);
        return value == null
                ? LookupResult.notFound("REFERENCE_RATE_NOT_FOUND")
                : LookupResult.found(new ReferenceRate(
                        assetNetwork.asset(), quoteCurrency.toUpperCase(), value, asOf, "reference_rates.json"
                ), "reference_rates.json:" + pair);
    }

    private Map<String, java.math.BigDecimal> load(Path file, ObjectMapper mapper) {
        try {
            JsonNode root = mapper.readTree(file.toFile());
            Map<String, java.math.BigDecimal> loaded = new HashMap<>();
            root.fields().forEachRemaining(entry -> loaded.put(entry.getKey(), entry.getValue().decimalValue()));
            return java.util.Collections.unmodifiableMap(new java.util.HashMap<>(loaded));
        } catch (IOException | RuntimeException exception) {
            throw new IllegalStateException("Cannot load reference-rate file: " + file, exception);
        }
    }
}
