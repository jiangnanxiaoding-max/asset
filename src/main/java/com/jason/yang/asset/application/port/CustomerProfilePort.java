package com.jason.yang.asset.application.port;

import com.jason.yang.asset.domain.CustomerProfile;
import com.jason.yang.asset.domain.CustomerId;
import com.jason.yang.asset.domain.LookupResult;

public interface CustomerProfilePort {
    LookupResult<CustomerProfile> findCustomer(CustomerId customerId);
}
