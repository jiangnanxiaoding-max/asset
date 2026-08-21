package com.jason.yang.asset.infrastructure.config;

import com.jason.yang.asset.application.TriageOrderUseCase;
import com.jason.yang.asset.application.batch.ProcessOrderBatchUseCase;
import com.jason.yang.asset.application.input.OrderParser;

/** Application components shared by the CLI and Web inbound adapters. */
public final class OfflineTriageRuntime {
    private final ProcessOrderBatchUseCase batchUseCase;
    private final TriageOrderUseCase triageOrderUseCase;
    private final OrderParser orderParser;

    public OfflineTriageRuntime(ProcessOrderBatchUseCase batchUseCase, TriageOrderUseCase triageOrderUseCase, OrderParser orderParser) {

        this.batchUseCase = batchUseCase;
        this.triageOrderUseCase = triageOrderUseCase;
        this.orderParser = orderParser;
    }

    public ProcessOrderBatchUseCase batchUseCase() {
        return batchUseCase;
    }

    public ProcessOrderBatchUseCase getBatchUseCase() {
        return batchUseCase;
    }

    public TriageOrderUseCase triageOrderUseCase() {
        return triageOrderUseCase;
    }

    public TriageOrderUseCase getTriageOrderUseCase() {
        return triageOrderUseCase;
    }

    public OrderParser orderParser() {
        return orderParser;
    }

    public OrderParser getOrderParser() {
        return orderParser;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof OfflineTriageRuntime)) return false;
        OfflineTriageRuntime that = (OfflineTriageRuntime) other;
        return java.util.Objects.equals(batchUseCase, that.batchUseCase)
                && java.util.Objects.equals(triageOrderUseCase, that.triageOrderUseCase)
                && java.util.Objects.equals(orderParser, that.orderParser);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(batchUseCase, triageOrderUseCase, orderParser);
    }

    @Override
    public String toString() {
        return "OfflineTriageRuntime{" + "batchUseCase=" + batchUseCase + ", triageOrderUseCase=" + triageOrderUseCase + ", orderParser=" + orderParser + "}";
    }


}
