package com.jason.yang.asset.infrastructure.config;

import com.jason.yang.asset.application.batch.RunTriageQueueUseCase;
import com.jason.yang.asset.application.evaluation.EvaluateTriageUseCase;
import com.jason.yang.asset.application.port.BatchProcessorProvider;
import com.jason.yang.asset.application.service.DefaultRunTriageQueueService;
import com.jason.yang.asset.infrastructure.evaluation.JsonGoldenEvaluationService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Shared composition root for the CLI and Web inbound adapters. */
@Configuration
public class TriageApplicationConfiguration {

    @Bean
    OfflineTriageRuntimeFactory offlineTriageRuntimeFactory() {
        return new OfflineTriageRuntimeFactory();
    }

    @Bean
    BatchProcessorProvider batchProcessorProvider(OfflineTriageRuntimeFactory runtimeFactory) {
        return new CachingOfflineBatchProcessorProvider(runtimeFactory);
    }

    @Bean
    RunTriageQueueUseCase runTriageQueueUseCase(BatchProcessorProvider processorProvider) {
        return new DefaultRunTriageQueueService(processorProvider);
    }

    @Bean
    EvaluateTriageUseCase evaluateTriageUseCase(OfflineTriageRuntimeFactory runtimeFactory) {
        return new JsonGoldenEvaluationService(runtimeFactory);
    }
}
