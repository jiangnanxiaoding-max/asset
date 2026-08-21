package com.jason.yang.asset.application.batch;

/** Shared application entry point used by CLI and HTTP queue adapters. */
public interface RunTriageQueueUseCase {
    RunTriageQueueResult run();
}
