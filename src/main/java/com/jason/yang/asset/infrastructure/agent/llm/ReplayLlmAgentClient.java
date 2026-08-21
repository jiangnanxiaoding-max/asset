package com.jason.yang.asset.infrastructure.agent.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;

/** Offline JSONL cassette client for repeatable Agent evaluation. */
public class ReplayLlmAgentClient implements LlmAgentClient {
    private final Deque<Response> responses;

    public ReplayLlmAgentClient(Path cassette, ObjectMapper mapper) {
        this.responses = load(cassette, mapper);
    }

    @Override
    public synchronized Response next(Request request, Duration timeout) {
        if (responses.isEmpty()) {
            throw new ClientException("Replay cassette exhausted", false);
        }
        return responses.removeFirst();
    }

    @Override
    public String provider() { return "replay"; }

    @Override
    public String model() { return "offline-replay-v1"; }

    private Deque<Response> load(Path cassette, ObjectMapper mapper) {
        Deque<Response> loaded = new ArrayDeque<Response>();
        try {
            for (String line : Files.readAllLines(cassette, StandardCharsets.UTF_8)) {
                if (line.trim().isEmpty()) continue;
                JsonNode node = mapper.readTree(line);
                String type = requiredText(node, "type");
                int tokens = node.path("tokens_used").asInt(0);
                if ("tool_call".equals(type)) {
                    loaded.addLast(new ToolCall(requiredText(node, "tool_name"), tokens));
                } else if ("finish".equals(type)) {
                    loaded.addLast(new Finish(tokens));
                } else {
                    throw new IllegalArgumentException("Unsupported replay response type: " + type);
                }
            }
        } catch (IOException exception) {
            throw new IllegalArgumentException("Cannot read LLM replay cassette: " + cassette, exception);
        }
        return loaded;
    }

    private String requiredText(JsonNode node, String name) {
        String value = node.path(name).asText("");
        if (value.trim().isEmpty()) throw new IllegalArgumentException("Missing replay field: " + name);
        return value;
    }
}
