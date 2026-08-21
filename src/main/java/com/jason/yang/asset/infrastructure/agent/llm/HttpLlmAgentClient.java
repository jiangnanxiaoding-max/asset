package com.jason.yang.asset.infrastructure.agent.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** HTTP adapter for a controlled LLM gateway returning tool_call/finish structured JSON. */
public class HttpLlmAgentClient implements LlmAgentClient {
    private static final int MAX_RESPONSE_BYTES = 65_536;
    private final URL endpoint;
    private final String apiKey;
    private final String model;
    private final ObjectMapper mapper;

    public HttpLlmAgentClient(String endpoint, String apiKey, String model, ObjectMapper mapper) {
        try {
            this.endpoint = new URL(endpoint);
        } catch (Exception exception) {
            throw new IllegalArgumentException("Invalid ASSET_LLM_ENDPOINT", exception);
        }
        if (!"https".equalsIgnoreCase(this.endpoint.getProtocol())
                && !"http".equalsIgnoreCase(this.endpoint.getProtocol())) {
            throw new IllegalArgumentException("LLM endpoint must use HTTP or HTTPS");
        }
        this.apiKey = required(apiKey, "ASSET_LLM_API_KEY");
        this.model = required(model, "ASSET_LLM_MODEL");
        this.mapper = mapper;
    }

    @Override
    public Response next(Request request, Duration timeout) {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) endpoint.openConnection();
            int timeoutMillis = boundedMillis(timeout);
            connection.setConnectTimeout(timeoutMillis);
            connection.setReadTimeout(timeoutMillis);
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Authorization", "Bearer " + apiKey);
            byte[] body = mapper.writeValueAsBytes(requestBody(request));
            connection.setFixedLengthStreamingMode(body.length);
            try (OutputStream output = connection.getOutputStream()) {
                output.write(body);
            }

            int status = connection.getResponseCode();
            boolean retryable = status == 429 || status >= 500;
            InputStream stream = status >= 200 && status < 300
                    ? connection.getInputStream() : connection.getErrorStream();
            byte[] responseBytes = readLimited(stream);
            if (status < 200 || status >= 300) {
                throw new ClientException("LLM gateway HTTP status " + status, retryable);
            }
            JsonNode response = mapper.readTree(new String(responseBytes, StandardCharsets.UTF_8));
            return parse(response);
        } catch (ClientException exception) {
            throw exception;
        } catch (IOException exception) {
            throw new ClientException("LLM gateway I/O failure", true, exception);
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    @Override
    public String provider() { return "http"; }

    @Override
    public String model() { return model; }

    private Map<String, Object> requestBody(Request request) {
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("model", model);
        body.put("prompt_version", "investigation-v1");
        body.put("order_id", request.orderId());
        body.put("order_type", request.orderType());
        body.put("asset", request.asset());
        body.put("network", request.network());
        body.put("policy_version", request.policyVersion());
        body.put("iteration", request.iteration());
        body.put("remaining_tool_calls", request.remainingToolCalls());
        body.put("remaining_tokens", request.remainingTokens());
        body.put("missing_facts", request.missingFacts());
        body.put("completed_tools", request.completedTools());
        body.put("observations", request.observationSummaries());
        List<Map<String, Object>> toolSchemas = new ArrayList<Map<String, Object>>();
        for (AgentToolDefinition definition : request.availableTools()) {
            Map<String, Object> schema = new LinkedHashMap<String, Object>();
            schema.put("name", definition.name());
            schema.put("description", definition.description());
            schema.put("produces_fact", definition.producesFact().name());
            schema.put("prerequisites", definition.prerequisites());
            toolSchemas.add(schema);
        }
        body.put("tools", toolSchemas);
        return body;
    }

    private Response parse(JsonNode node) {
        String type = requiredText(node, "type");
        int tokens = node.path("tokens_used").asInt(-1);
        if (tokens < 0) throw new ClientException("Invalid tokens_used", false);
        if ("tool_call".equals(type)) {
            return new ToolCall(requiredText(node, "tool_name"), tokens);
        }
        if ("finish".equals(type)) return new Finish(tokens);
        throw new ClientException("Unsupported structured response type", false);
    }

    private byte[] readLimited(InputStream input) throws IOException {
        if (input == null) return new byte[0];
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int read;
        int total = 0;
        while ((read = input.read(buffer)) >= 0) {
            total += read;
            if (total > MAX_RESPONSE_BYTES) {
                throw new ClientException("LLM response exceeds size limit", false);
            }
            output.write(buffer, 0, read);
        }
        return output.toByteArray();
    }

    private int boundedMillis(Duration timeout) {
        long millis = Math.max(1L, timeout.toMillis());
        return (int) Math.min(Integer.MAX_VALUE, millis);
    }

    private String requiredText(JsonNode node, String field) {
        String value = node.path(field).asText("");
        if (value.trim().isEmpty()) throw new ClientException("Missing field " + field, false);
        return value;
    }

    private String required(String value, String name) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(name + " is required for the HTTP LLM provider");
        }
        return value;
    }
}
