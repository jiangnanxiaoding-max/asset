package com.jason.yang.asset.infrastructure.reference;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jason.yang.asset.application.port.AddressRiskPort;
import com.jason.yang.asset.domain.AddressRiskAssessment;
import com.jason.yang.asset.domain.BlockchainAddress;
import com.jason.yang.asset.domain.LookupResult;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/** Offline address-risk provider backed by the authoritative snapshot. */
public final class FileAddressRiskAdapter implements AddressRiskPort {
    private final Map<String, AddressRiskAssessment> assessments;

    public FileAddressRiskAdapter(Path file, ObjectMapper mapper, Instant snapshotTime) {
        this.assessments = load(file, mapper, snapshotTime);
    }

    @Override
    public LookupResult<AddressRiskAssessment> screen(BlockchainAddress address) {
        AddressRiskAssessment assessment = assessments.get(address.value());
        return assessment == null
                ? LookupResult.notFound("ADDRESS_NOT_FOUND")
                : LookupResult.found(assessment, "address_risk.json:" + address.value());
    }

    private Map<String, AddressRiskAssessment> load(Path file, ObjectMapper mapper, Instant snapshotTime) {
        try {
            JsonNode root = mapper.readTree(file.toFile());
            Map<String, AddressRiskAssessment> loaded = new HashMap<>();
            root.fields().forEachRemaining(entry -> {
                JsonNode node = entry.getValue();
                AddressRiskAssessment.RiskCategory category;
                try {
                    category = AddressRiskAssessment.RiskCategory.valueOf(
                            node.path("category").asText("unknown").toUpperCase(Locale.ROOT));
                } catch (IllegalArgumentException exception) {
                    category = AddressRiskAssessment.RiskCategory.OTHER;
                }
                loaded.put(entry.getKey(), new AddressRiskAssessment(
                        node.path("risk_score").asInt(), category, snapshotTime,
                        "file-snapshot:" + entry.getKey()
                ));
            });
            return java.util.Collections.unmodifiableMap(new java.util.HashMap<>(loaded));
        } catch (IOException | RuntimeException exception) {
            throw new IllegalStateException("Cannot load address-risk file: " + file, exception);
        }
    }
}
