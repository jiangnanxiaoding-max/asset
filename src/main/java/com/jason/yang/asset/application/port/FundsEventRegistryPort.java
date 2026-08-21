package com.jason.yang.asset.application.port;

import com.jason.yang.asset.domain.DuplicateAssessment;
import com.jason.yang.asset.domain.LookupResult;
import com.jason.yang.asset.domain.OffRampOrder;

public interface FundsEventRegistryPort {
    /** Atomically claims the order's chain event or reports its prior owner. */
    LookupResult<DuplicateAssessment> assess(OffRampOrder order);
}
