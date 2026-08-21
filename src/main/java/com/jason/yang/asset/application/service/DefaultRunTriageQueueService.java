package com.jason.yang.asset.application.service;

import com.jason.yang.asset.application.batch.BatchCommand;
import com.jason.yang.asset.application.batch.BatchResult;
import com.jason.yang.asset.application.batch.ProcessOrderBatchUseCase;
import com.jason.yang.asset.application.batch.RunTriageQueueResult;
import com.jason.yang.asset.application.batch.RunTriageQueueUseCase;
import com.jason.yang.asset.application.port.BatchProcessorProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;

/** Coordinates a configured queue run while keeping runtime creation out of inbound adapters. */
public class DefaultRunTriageQueueService implements RunTriageQueueUseCase {
    private static final Logger log = LoggerFactory.getLogger(DefaultRunTriageQueueService.class);
    private static final Path MATERIALS_DIRECTORY = normalized("materials");
    private static final Path ORDERS_FILE = normalized("materials/orders.jsonl");
    private static final Path OUTPUT_FILE = normalized("build/decisions.jsonl");
    private static final Path AUDIT_FILE = normalized("build/audit.jsonl");
    private static final Instant EVALUATION_TIME = Instant.parse("2026-07-28T12:00:00Z");
    private static final int MAX_CONCURRENCY = 1;

    private final BatchProcessorProvider processorProvider;
    private final ReentrantLock executionLock = new ReentrantLock();

    public DefaultRunTriageQueueService(BatchProcessorProvider processorProvider) {
        this.processorProvider = Objects.requireNonNull(processorProvider);
    }

    @Override
    public RunTriageQueueResult run() {
        String runId = "run-" + UUID.randomUUID();
        log.info("queue execution requested runId={} orders={} maxConcurrency={}",
                runId, ORDERS_FILE.getFileName(), MAX_CONCURRENCY);
        executionLock.lock();
        try {
            ProcessOrderBatchUseCase processor = processorProvider.get(
                    MATERIALS_DIRECTORY, AUDIT_FILE, EVALUATION_TIME);
            BatchResult result = processor.process(new BatchCommand(
                    ORDERS_FILE, OUTPUT_FILE, runId, EVALUATION_TIME, MAX_CONCURRENCY));
            log.info("queue execution completed runId={} total={} failed={} elapsedMs={}",
                    result.runId(), result.total(), result.failed(), result.elapsed().toMillis());
            return new RunTriageQueueResult(result, OUTPUT_FILE, AUDIT_FILE);
        } catch (RuntimeException exception) {
            log.error("queue execution failed runId={}", runId, exception);
            throw exception;
        } finally {
            executionLock.unlock();
        }
    }

    private static Path normalized(String path) {
        return Paths.get(path).toAbsolutePath().normalize();
    }
}
