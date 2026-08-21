package com.jason.yang.asset.application.evaluation;

/** Executes deterministic golden cases and explicitly counts unsafe automatic completions. */
public interface EvaluateTriageUseCase {
    EvaluationReport evaluate(EvaluationCommand command);
}
