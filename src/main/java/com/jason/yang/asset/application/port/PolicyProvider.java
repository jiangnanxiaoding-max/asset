package com.jason.yang.asset.application.port;

import com.jason.yang.asset.domain.PolicySnapshot;

/** Supplies the immutable policy version used for a single decision. */
public interface PolicyProvider {
    PolicySnapshot currentPolicy();
}
