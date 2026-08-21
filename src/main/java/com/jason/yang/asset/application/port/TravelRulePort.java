package com.jason.yang.asset.application.port;

import com.jason.yang.asset.domain.LookupResult;
import com.jason.yang.asset.domain.Order;
import com.jason.yang.asset.domain.TravelRuleAssessment;

import java.math.BigDecimal;

public interface TravelRulePort {
    LookupResult<TravelRuleAssessment> assess(Order order, BigDecimal usdEquivalent);
}
