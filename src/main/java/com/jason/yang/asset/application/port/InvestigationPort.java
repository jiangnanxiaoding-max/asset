package com.jason.yang.asset.application.port;

import com.jason.yang.asset.domain.InvestigationFacts;
import com.jason.yang.asset.domain.Order;
import com.jason.yang.asset.domain.PolicySnapshot;

/** Collects a versioned fact snapshot required by domain policies. */
public interface InvestigationPort {
    InvestigationFacts investigate(Order order, PolicySnapshot policy);
}
