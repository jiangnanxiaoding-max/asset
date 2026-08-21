package com.jason.yang.asset.infrastructure.evaluation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.jason.yang.asset.application.batch.BatchCommand;
import com.jason.yang.asset.application.evaluation.EvaluateTriageUseCase;
import com.jason.yang.asset.application.evaluation.EvaluationCommand;
import com.jason.yang.asset.application.evaluation.EvaluationFailure;
import com.jason.yang.asset.application.evaluation.EvaluationReport;
import com.jason.yang.asset.infrastructure.config.OfflineTriageRuntimeFactory;
import com.jason.yang.asset.infrastructure.config.OfflineTriageRuntime;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Golden evaluator adapter; structured decisions, never natural-language wording, determine pass/fail. */
public final class JsonGoldenEvaluationService implements EvaluateTriageUseCase {
    private static final Instant EVALUATION_TIME = Instant.parse("2026-07-28T12:00:00Z");
    private final OfflineTriageRuntimeFactory runtimeFactory;
    private final ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    public JsonGoldenEvaluationService(OfflineTriageRuntimeFactory runtimeFactory) {
        this.runtimeFactory = runtimeFactory;
    }

    @Override
    public EvaluationReport evaluate(EvaluationCommand command) {
        Path workDirectory;
        try {
            workDirectory = Files.createTempDirectory("asset-evaluation-");
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot create evaluation workspace", exception);
        }
        Path output = workDirectory.resolve("decisions.jsonl");
        Path audit = workDirectory.resolve("audit.jsonl");
        OfflineTriageRuntime runtime = runtimeFactory.create(command.materialsDirectory(), audit, EVALUATION_TIME);
        runtime.batchUseCase().process(new BatchCommand(
                command.materialsDirectory().resolve("orders.jsonl"), output,
                "eval-" + UUID.randomUUID(), EVALUATION_TIME, 1));

        Map<String, ExpectedCase> expected = loadGolden(command.goldenCases());
        Map<String, JsonNode> actual = loadActual(output);
        List<EvaluationFailure> failures = new ArrayList<>();
        int passed = 0;
        int unsafe = 0;
        for (Map.Entry<String, ExpectedCase> entry : expected.entrySet()) {
            JsonNode observed = actual.get(entry.getKey());
            ExpectedCase golden = entry.getValue();
            if (observed == null) {
                failures.add(new EvaluationFailure(entry.getKey(), golden.disposition(),
                        "MISSING", "No decision was produced"));
                continue;
            }
            String disposition = observed.path("disposition").asText();
            List<String> reasons = new ArrayList<>();
            observed.path("reason_codes").forEach(node -> reasons.add(node.asText()));
            boolean dispositionMatches = golden.disposition().equals(disposition);
            boolean reasonsMatch = reasons.containsAll(golden.requiredReasons());
            if (dispositionMatches && reasonsMatch) {
                passed++;
            } else {
                failures.add(new EvaluationFailure(entry.getKey(), golden.disposition(), disposition,
                        "requiredReasons=" + golden.requiredReasons() + ", actualReasons=" + reasons));
            }
            if ("AUTO_COMPLETE".equals(disposition) && !golden.autoCompleteSafe()) {
                unsafe++;
            }
        }
        EvaluationReport report = new EvaluationReport(
                expected.size(), passed, failures.size(), unsafe, failures);
        writeReport(command.reportFile(), report);
        return report;
    }

    private Map<String, ExpectedCase> loadGolden(Path file) {
        try {
            JsonNode root = mapper.readTree(file.toFile());
            Map<String, ExpectedCase> result = new LinkedHashMap<>();
            root.fields().forEachRemaining(entry -> {
                JsonNode node = entry.getValue();
                List<String> reasons = new ArrayList<>();
                node.path("required_reason_codes").forEach(reason -> reasons.add(reason.asText()));
                result.put(entry.getKey(), new ExpectedCase(
                        node.path("disposition").asText(), reasons,
                        node.path("auto_complete_safe").asBoolean(false)));
            });
            return result;
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot load golden cases: " + file, exception);
        }
    }

    private Map<String, JsonNode> loadActual(Path file) {
        try {
            Map<String, JsonNode> result = new HashMap<>();
            for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                if (!line.trim().isEmpty()) {
                    JsonNode node = mapper.readTree(line);
                    result.put(node.path("order_id").asText(), node);
                }
            }
            return result;
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot read evaluation decisions", exception);
        }
    }

    private void writeReport(Path file, EvaluationReport report) {
        try {
            Path normalized = file.toAbsolutePath().normalize();
            if (normalized.getParent() != null) {
                Files.createDirectories(normalized.getParent());
            }
            mapper.writeValue(normalized.toFile(), report);
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot write evaluation report", exception);
        }
    }

    private static final class ExpectedCase {
    private final String disposition;
    private final List<String> requiredReasons;
    private final boolean autoCompleteSafe;

    public ExpectedCase(String disposition, List<String> requiredReasons, boolean autoCompleteSafe) {

        this.disposition = disposition;
        this.requiredReasons = requiredReasons;
        this.autoCompleteSafe = autoCompleteSafe;
    }

    public String disposition() {
        return disposition;
    }

    public String getDisposition() {
        return disposition;
    }

    public List<String> requiredReasons() {
        return requiredReasons;
    }

    public List<String> getRequiredReasons() {
        return requiredReasons;
    }

    public boolean autoCompleteSafe() {
        return autoCompleteSafe;
    }

    public boolean getAutoCompleteSafe() {
        return autoCompleteSafe;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof ExpectedCase)) return false;
        ExpectedCase that = (ExpectedCase) other;
        return java.util.Objects.equals(disposition, that.disposition)
                && java.util.Objects.equals(requiredReasons, that.requiredReasons)
                && autoCompleteSafe == that.autoCompleteSafe;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(disposition, requiredReasons, autoCompleteSafe);
    }

    @Override
    public String toString() {
        return "ExpectedCase{" + "disposition=" + disposition + ", requiredReasons=" + requiredReasons + ", autoCompleteSafe=" + autoCompleteSafe + "}";
    }


    }
}
