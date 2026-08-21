package com.jason.yang.asset.infrastructure.persistence;

import com.jason.yang.asset.domain.TriageCase;
import com.jason.yang.asset.domain.repository.TriageCaseRepository;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** In-memory aggregate repository; stores immutable lifecycle snapshots by aggregate identity. */
public final class InMemoryTriageCaseRepository implements TriageCaseRepository {
    private final Map<String, TriageCase> cases = new ConcurrentHashMap<>();

    @Override
    public void save(TriageCase triageCase) {
        cases.put(triageCase.id().value(), triageCase);
    }

    public TriageCase find(String orderId) {
        return cases.get(orderId);
    }
}
