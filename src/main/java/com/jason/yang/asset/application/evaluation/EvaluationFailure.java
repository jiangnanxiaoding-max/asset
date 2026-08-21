package com.jason.yang.asset.application.evaluation;

public final class EvaluationFailure {
    private final String orderId;
    private final String expected;
    private final String actual;
    private final String detail;

    public EvaluationFailure(String orderId, String expected, String actual, String detail) {

        this.orderId = orderId;
        this.expected = expected;
        this.actual = actual;
        this.detail = detail;
    }

    public String orderId() {
        return orderId;
    }

    public String getOrderId() {
        return orderId;
    }

    public String expected() {
        return expected;
    }

    public String getExpected() {
        return expected;
    }

    public String actual() {
        return actual;
    }

    public String getActual() {
        return actual;
    }

    public String detail() {
        return detail;
    }

    public String getDetail() {
        return detail;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof EvaluationFailure)) return false;
        EvaluationFailure that = (EvaluationFailure) other;
        return java.util.Objects.equals(orderId, that.orderId)
                && java.util.Objects.equals(expected, that.expected)
                && java.util.Objects.equals(actual, that.actual)
                && java.util.Objects.equals(detail, that.detail);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(orderId, expected, actual, detail);
    }

    @Override
    public String toString() {
        return "EvaluationFailure{" + "orderId=" + orderId + ", expected=" + expected + ", actual=" + actual + ", detail=" + detail + "}";
    }


}
