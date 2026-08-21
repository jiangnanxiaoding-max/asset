package com.jason.yang.asset.domain.repository;

import com.jason.yang.asset.domain.TriageCase;

/** Persists the aggregate at transaction boundaries without exposing storage models. */
@FunctionalInterface
public interface TriageCaseRepository {
    void save(TriageCase triageCase);
}
