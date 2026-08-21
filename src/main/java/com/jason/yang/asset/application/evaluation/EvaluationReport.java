package com.jason.yang.asset.application.evaluation;

import java.util.List;

public final class EvaluationReport {
    private final int cases;
    private final int passed;
    private final int failed;
    private final int unsafeAutoCompletions;
    private final List<EvaluationFailure> failures;

    public int cases() {
        return cases;
    }

    public int getCases() {
        return cases;
    }

    public int passed() {
        return passed;
    }

    public int getPassed() {
        return passed;
    }

    public int failed() {
        return failed;
    }

    public int getFailed() {
        return failed;
    }

    public int unsafeAutoCompletions() {
        return unsafeAutoCompletions;
    }

    public int getUnsafeAutoCompletions() {
        return unsafeAutoCompletions;
    }

    public List<EvaluationFailure> failures() {
        return failures;
    }

    public List<EvaluationFailure> getFailures() {
        return failures;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof EvaluationReport)) return false;
        EvaluationReport that = (EvaluationReport) other;
        return cases == that.cases
                && passed == that.passed
                && failed == that.failed
                && unsafeAutoCompletions == that.unsafeAutoCompletions
                && java.util.Objects.equals(failures, that.failures);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(cases, passed, failed, unsafeAutoCompletions, failures);
    }

    @Override
    public String toString() {
        return "EvaluationReport{" + "cases=" + cases + ", passed=" + passed + ", failed=" + failed + ", unsafeAutoCompletions=" + unsafeAutoCompletions + ", failures=" + failures + "}";
    }


    public EvaluationReport(int cases, int passed, int failed, int unsafeAutoCompletions, List<EvaluationFailure> failures) {
        failures = java.util.Collections.unmodifiableList(new java.util.ArrayList<>(failures));
    

        this.cases = cases;

        this.passed = passed;

        this.failed = failed;

        this.unsafeAutoCompletions = unsafeAutoCompletions;

        this.failures = failures;

    }

    public boolean successful() {
        return failed == 0 && unsafeAutoCompletions == 0;
    }
}
