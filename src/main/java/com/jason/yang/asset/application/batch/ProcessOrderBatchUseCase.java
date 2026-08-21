package com.jason.yang.asset.application.batch;

/** Processes every JSONL line independently and never lets a bad order stop the remaining batch. */
public interface ProcessOrderBatchUseCase {
    BatchResult process(BatchCommand command);
}
