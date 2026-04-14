package com.jobinesh.ai.example;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.context.annotation.Value;
import jakarta.inject.Singleton;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

@Singleton
class McpTaskClient {
    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
    private final ObjectMapper objectMapper;
    private final URI mcpUri;
    private final AtomicLong id = new AtomicLong(1);
    private final AtomicBoolean initialized = new AtomicBoolean(false);

    McpTaskClient(ObjectMapper objectMapper,
                  @Value("${agent.mcp.url}") String mcpUrl) {
        this.objectMapper = objectMapper;
        this.mcpUri = URI.create(mcpUrl);
    }

    JsonNode callTool(String tool, Map<String, Object> arguments) {
        initializeIfNeeded();
        return rpc("tools/call", Map.of("name", tool, "arguments", arguments));
    }

    private void initializeIfNeeded() {
        if (initialized.get()) {
            return;
        }
        rpc("initialize", Map.of(
            "protocolVersion", "2024-11-05",
            "capabilities", Map.of(),
            "clientInfo", Map.of("name", "task-agent-module", "version", "1.0")
        ));
        initialized.set(true);
    }

    private JsonNode rpc(String method, Map<String, Object> params) {
        try {
            String payload = objectMapper.writeValueAsString(Map.of(
                "jsonrpc", "2.0",
                "id", id.incrementAndGet(),
                "method", method,
                "params", params
            ));
            HttpRequest req = HttpRequest.newBuilder(mcpUri)
                .timeout(Duration.ofSeconds(15))
                .header("content-type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();
            HttpResponse<String> res = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            JsonNode root = objectMapper.readTree(res.body());
            if (root.has("error") && !root.get("error").isNull()) {
                throw new IllegalStateException(root.get("error").toString());
            }
            return root.get("result");
        } catch (Exception e) {
            throw new IllegalStateException("MCP request failed: " + e.getMessage(), e);
        }
    }
}
