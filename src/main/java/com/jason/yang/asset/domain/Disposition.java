package com.jason.yang.asset.domain;

/**
 * audit result结果集
 */
public enum Disposition {
    AUTO_COMPLETE,
    HOLD,
    FREEZE_COMPLIANCE,
    REJECT_ESCALATE,
    REQUOTE,
    REFUND_REVIEW,
    OPS_RECOVERY,
    DUPLICATE_NOOP,
    MANUAL_REVIEW,
    INVALID_INPUT
}
