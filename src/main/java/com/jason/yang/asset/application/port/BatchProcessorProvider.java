package com.jason.yang.asset.application.port;

import com.jason.yang.asset.application.batch.ProcessOrderBatchUseCase;

import java.nio.file.Path;
import java.time.Instant;

/** Supplies a batch processor whose infrastructure state is scoped to one runtime configuration. */
public interface BatchProcessorProvider {
    ProcessOrderBatchUseCase get(Path materialsDirectory, Path auditFile, Instant evaluationTime);
}
