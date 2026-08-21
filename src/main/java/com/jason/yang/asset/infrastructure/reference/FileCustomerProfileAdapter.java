package com.jason.yang.asset.infrastructure.reference;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jason.yang.asset.application.port.CustomerProfilePort;
import com.jason.yang.asset.domain.CustomerId;
import com.jason.yang.asset.domain.CustomerProfile;
import com.jason.yang.asset.domain.LookupResult;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/** Immutable customer reference-data adapter loaded from the interview material. */
public final class FileCustomerProfileAdapter implements CustomerProfilePort {
    private final Map<String, CustomerProfile> customers;

    public FileCustomerProfileAdapter(Path file, ObjectMapper mapper) {
        this.customers = load(file, mapper);
    }

    @Override
    public LookupResult<CustomerProfile> findCustomer(CustomerId customerId) {
        CustomerProfile profile = customers.get(customerId.value());
        return profile == null
                ? LookupResult.notFound("CUSTOMER_NOT_FOUND")
                : LookupResult.found(profile, "customers.json:" + customerId.value());
    }

    private Map<String, CustomerProfile> load(Path file, ObjectMapper mapper) {
        try {
            JsonNode root = mapper.readTree(file.toFile());
            Map<String, CustomerProfile> loaded = new HashMap<>();
            root.fields().forEachRemaining(entry -> {
                JsonNode node = entry.getValue();
                CustomerProfile.Status status;
                try {
                    status = CustomerProfile.Status.valueOf(node.path("status").asText("unknown")
                            .toUpperCase(Locale.ROOT));
                } catch (IllegalArgumentException exception) {
                    status = CustomerProfile.Status.UNKNOWN;
                }
                loaded.put(entry.getKey(), new CustomerProfile(
                        entry.getKey(),
                        node.path("name").asText(),
                        node.path("kyc_tier").asInt(),
                        node.path("monthly_limit_usd").decimalValue(),
                        Optional.empty(),
                        node.path("verified_bank_name").asText(),
                        status
                ));
            });
            return java.util.Collections.unmodifiableMap(new java.util.HashMap<>(loaded));
        } catch (IOException | RuntimeException exception) {
            throw new IllegalStateException("Cannot load customer reference file: " + file, exception);
        }
    }
}
