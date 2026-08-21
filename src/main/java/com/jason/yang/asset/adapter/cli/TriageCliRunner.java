package com.jason.yang.asset.adapter.cli;

import com.jason.yang.asset.application.batch.BatchResult;
import com.jason.yang.asset.application.batch.RunTriageQueueResult;
import com.jason.yang.asset.application.batch.RunTriageQueueUseCase;
import com.jason.yang.asset.application.evaluation.EvaluateTriageUseCase;
import com.jason.yang.asset.application.evaluation.EvaluationCommand;
import com.jason.yang.asset.application.evaluation.EvaluationReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** CLI adapter exposing the offline triage and evaluation use cases. */
@Component
@Profile("cli")
public final class TriageCliRunner implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(TriageCliRunner.class);
    private final RunTriageQueueUseCase runTriageQueueUseCase;
    private final EvaluateTriageUseCase evaluationUseCase;

    public TriageCliRunner(RunTriageQueueUseCase runTriageQueueUseCase,
                           EvaluateTriageUseCase evaluationUseCase) {
        this.runTriageQueueUseCase = runTriageQueueUseCase;
        this.evaluationUseCase = evaluationUseCase;
    }

    @Override
    public void run(ApplicationArguments arguments) {
        List<String> sourceArgs = arguments.getSourceArgs().length == 0
                ? Collections.<String>emptyList() : Arrays.asList(arguments.getSourceArgs());
        if (sourceArgs.isEmpty()) {
            log.info("No CLI command supplied. Use 'triage' or 'evaluate'.");
            return;
        }
        String command = sourceArgs.get(0);
        Map<String, String> options = parseOptions(sourceArgs.subList(1, sourceArgs.size()));
        if ("triage".equals(command)) {
            triage();
        } else if ("evaluate".equals(command)) {
            evaluate(options);
        } else {
            throw new IllegalArgumentException("Unknown command: " + command);
        }
    }

    private void triage() {
        RunTriageQueueResult execution = runTriageQueueUseCase.run();
        BatchResult result = execution.batchResult();
        log.info("triage command completed runId={} total={} failed={} dispositionCounts={}",
                result.runId(), result.total(), result.failed(), result.dispositionCounts());
    }

    private void evaluate(Map<String, String> options) {
        Path materials = path(options, "materials", "materials");
        Path golden = path(options, "golden", "evaluation/golden-cases.json");
        Path report = path(options, "report", "build/evaluation-report.json");
        EvaluationReport result = evaluationUseCase.evaluate(
                new EvaluationCommand(materials, golden, report));
        log.info("evaluation completed cases={} passed={} failed={} unsafeAutoCompletions={}",
                result.cases(), result.passed(), result.failed(), result.unsafeAutoCompletions());
        if (!result.successful()) {
            throw new IllegalStateException("Evaluation failed; inspect " + report);
        }
    }

    private Map<String, String> parseOptions(List<String> args) {
        Map<String, String> options = new HashMap<>();
        for (int index = 0; index < args.size(); index += 2) {
            String key = args.get(index);
            if (!key.startsWith("--") || index + 1 >= args.size()) {
                throw new IllegalArgumentException("Options must use --name value pairs");
            }
            options.put(key.substring(2), args.get(index + 1));
        }
        return options;
    }

    private Path path(Map<String, String> options, String name, String defaultValue) {
        return java.nio.file.Paths.get(options.getOrDefault(name, defaultValue)).toAbsolutePath().normalize();
    }
}
