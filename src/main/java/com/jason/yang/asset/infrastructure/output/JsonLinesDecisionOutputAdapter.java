package com.jason.yang.asset.infrastructure.output;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.jason.yang.asset.application.batch.TriageOutputRecord;
import com.jason.yang.asset.application.port.DecisionOutputPort;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.locks.ReentrantLock;

/** Thread-safe JSONL decision sink using the public snake_case contract. */
public final class JsonLinesDecisionOutputAdapter implements DecisionOutputPort {
    private final ObjectMapper mapper;
    private final ReentrantLock lock = new ReentrantLock();

    public JsonLinesDecisionOutputAdapter() {
        this.mapper = JsonMapper.builder()
                .addModule(new JavaTimeModule())
                .propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
                .build();
    }

    @Override
    public void initialize(Path outputFile) {
        Path normalized = outputFile.toAbsolutePath().normalize();
        try {
            if (normalized.getParent() != null) {
                Files.createDirectories(normalized.getParent());
            }
            Files.write(normalized, new byte[0],
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot initialize decision output: " + normalized, exception);
        }
    }

    @Override
    public void append(Path outputFile, TriageOutputRecord record) {
        lock.lock();
        try {
            String json = mapper.writeValueAsString(record) + System.lineSeparator();
            Files.write(outputFile.toAbsolutePath().normalize(), json.getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot append decision output", exception);
        } finally {
            lock.unlock();
        }
    }
}
