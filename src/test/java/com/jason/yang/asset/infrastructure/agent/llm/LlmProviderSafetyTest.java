package com.jason.yang.asset.infrastructure.agent.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LlmProviderSafetyTest {

    @Test
    void missingProviderConfigurationSelectsOfflineStub() {
        LlmAgentClient client = new EnvironmentLlmAgentClientFactory(
                Collections.<String, String>emptyMap()).create(new ObjectMapper());

        assertEquals("stub", client.provider());
        assertEquals("offline-stub-v1", client.model());
    }

    @Test
    void realProviderCannotStartWithoutCredentials() {
        Map<String, String> environment = new HashMap<String, String>();
        environment.put("ASSET_LLM_PROVIDER", "http");

        assertThrows(IllegalArgumentException.class,
                () -> new EnvironmentLlmAgentClientFactory(environment).create(new ObjectMapper()));
    }

    @Test
    void decoratorEnforcesHardTimeoutEvenWhenDelegateDoesNot() {
        LlmAgentClient slow = new LlmAgentClient() {
            @Override
            public Response next(Request request, Duration timeout) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                }
                return new Finish(0);
            }
        };
        TimeoutEnforcingLlmAgentClient client = new TimeoutEnforcingLlmAgentClient(slow);

        LlmAgentClient.ClientException exception = assertThrows(LlmAgentClient.ClientException.class,
                () -> client.next(emptyRequest(), Duration.ofMillis(20)));

        assertTrue(exception.retryable());
    }

    private LlmAgentClient.Request emptyRequest() {
        return new LlmAgentClient.Request("O-TEST", "WithdrawalOrder", "BTC", "BTC", "policy", 1,
                1, 100, Collections.<AgentToolDefinition>emptyList(),
                Collections.<String>emptyList(), Collections.<String>emptyList(),
                Collections.<String>emptyList());
    }
}
