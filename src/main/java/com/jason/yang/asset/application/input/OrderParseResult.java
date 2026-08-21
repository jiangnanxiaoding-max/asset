package com.jason.yang.asset.application.input;

import com.jason.yang.asset.domain.Order;

import java.util.List;
import java.util.Optional;

public interface OrderParseResult {
final class Parsed implements OrderParseResult {
    private final Order order;

    public Parsed(Order order) {

        this.order = order;
    }

    public Order order() {
        return order;
    }

    public Order getOrder() {
        return order;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof Parsed)) return false;
        Parsed that = (Parsed) other;
        return java.util.Objects.equals(order, that.order);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(order);
    }

    @Override
    public String toString() {
        return "Parsed{" + "order=" + order + "}";
    }


    }static final class Invalid implements OrderParseResult {
    private final Optional<String> orderId;
    private final List<InputViolation> violations;

    public Optional<String> orderId() {
        return orderId;
    }

    public Optional<String> getOrderId() {
        return orderId;
    }

    public List<InputViolation> violations() {
        return violations;
    }

    public List<InputViolation> getViolations() {
        return violations;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof Invalid)) return false;
        Invalid that = (Invalid) other;
        return java.util.Objects.equals(orderId, that.orderId)
                && java.util.Objects.equals(violations, that.violations);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(orderId, violations);
    }

    @Override
    public String toString() {
        return "Invalid{" + "orderId=" + orderId + ", violations=" + violations + "}";
    }


        public Invalid(Optional<String> orderId, List<InputViolation> violations) {
            orderId = orderId == null ? Optional.empty() : orderId;
            violations = java.util.Collections.unmodifiableList(new java.util.ArrayList<>(violations));
        

            this.orderId = orderId;

            this.violations = violations;

        }
    }
}
