package com.jason.yang.asset.infrastructure.agent.llm;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.file.Paths;
import java.util.Map;

/** Creates an offline client by default and enables a paid HTTP provider only through explicit environment values. */
public class EnvironmentLlmAgentClientFactory {
    private final Map<String, String> environment;

    public EnvironmentLlmAgentClientFactory() {
        this(System.getenv());
    }

    public EnvironmentLlmAgentClientFactory(Map<String, String> environment) {
        this.environment = environment;
    }

    public LlmAgentClient create(ObjectMapper mapper) {
        String provider = value("ASSET_LLM_PROVIDER", "stub").toLowerCase(java.util.Locale.ROOT);
        LlmAgentClient client;
        if ("stub".equals(provider)) {
            client = new StubLlmAgentClient();
        } else if ("replay".equals(provider)) {
            client = new ReplayLlmAgentClient(
                    Paths.get(required("ASSET_LLM_REPLAY_FILE")), mapper);
        } else if ("http".equals(provider)) {
            client = new HttpLlmAgentClient(
                    required("ASSET_LLM_ENDPOINT"),
                    required("ASSET_LLM_API_KEY"),
                    required("ASSET_LLM_MODEL"),
                    mapper);
        } else {
            throw new IllegalArgumentException("Unsupported ASSET_LLM_PROVIDER: " + provider);
        }
        return new TimeoutEnforcingLlmAgentClient(client);
    }

    private String value(String key, String defaultValue) {
        String value = environment.get(key);
        return value == null || value.trim().isEmpty() ? defaultValue : value.trim();
    }

    private String required(String key) {
        String value = environment.get(key);
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(key + " is required for the selected LLM provider");
        }
        return value.trim();
    }
}
