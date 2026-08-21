package com.jason.yang.asset.infrastructure.config;

import com.jason.yang.asset.application.evaluation.EvaluateTriageUseCase;
import com.jason.yang.asset.infrastructure.evaluation.JsonGoldenEvaluationService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/** Web-mode composition root backed by the same offline adapters as the CLI exercise. */
@Configuration
@Profile("!cli")
public class WebTriageConfiguration {

    @Bean
    OfflineTriageRuntimeFactory offlineTriageRuntimeFactory() {
        return new OfflineTriageRuntimeFactory();
    }

    @Bean
    EvaluateTriageUseCase evaluateTriageUseCase(OfflineTriageRuntimeFactory runtimeFactory) {
        return new JsonGoldenEvaluationService(runtimeFactory);
    }
}
