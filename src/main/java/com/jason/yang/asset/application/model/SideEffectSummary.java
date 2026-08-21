package com.jason.yang.asset.application.model;

import java.util.List;

public final class SideEffectSummary {
    private final List<String> records;

    public List<String> records() {
        return records;
    }

    public List<String> getRecords() {
        return records;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof SideEffectSummary)) return false;
        SideEffectSummary that = (SideEffectSummary) other;
        return java.util.Objects.equals(records, that.records);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(records);
    }

    @Override
    public String toString() {
        return "SideEffectSummary{" + "records=" + records + "}";
    }


    public SideEffectSummary(List<String> records) {
        records = java.util.Collections.unmodifiableList(new java.util.ArrayList<>(records));
    

        this.records = records;

    }

    public static SideEffectSummary none() {
        return new SideEffectSummary(java.util.Collections.emptyList());
    }
}
