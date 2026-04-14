package com.jobinesh.ai.example;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Singleton;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@Singleton
class TaskAgentOrchestrator {
    private final TaskPlannerAiService planner;
    private final McpTaskClient mcpTaskClient;
    private final ObjectMapper objectMapper;

    TaskAgentOrchestrator(TaskPlannerAiService planner,
                          McpTaskClient mcpTaskClient,
                          ObjectMapper objectMapper) {
        this.planner = planner;
        this.mcpTaskClient = mcpTaskClient;
        this.objectMapper = objectMapper;
    }

    Map<String, Object> runInstruction(String instruction) {
        String raw = planner.plan(instruction);
        JsonNode call = parseToolCall(raw);

        String tool = call.path("tool").asText();
        if (tool.isBlank()) {
            throw new IllegalArgumentException("Model did not return tool");
        }

        Map<String, Object> args = jsonObjectToMap(call.path("arguments"));
        applyToolArgumentDefaults(tool, args);
        JsonNode result = mcpTaskClient.callTool(tool, args);

        Map<String, Object> out = new HashMap<>();
        out.put("tool", tool);
        out.put("arguments", args);
        out.put("mcpResult", result);
        out.put("rawModelOutput", raw);
        return out;
    }

    private void applyToolArgumentDefaults(String tool, Map<String, Object> args) {
        if ("create-task".equals(tool)) {
            args.putIfAbsent("description", "");
            args.putIfAbsent("priority", "MEDIUM");
            args.putIfAbsent("dueDate", "");
            args.putIfAbsent("tags", "");
            return;
        }

        if ("task-agent".equals(tool)) {
            args.putIfAbsent("command", "list");
            args.putIfAbsent("title", "");
            args.putIfAbsent("description", "");
            args.putIfAbsent("priority", "");
            args.putIfAbsent("dueDate", "");
            args.putIfAbsent("tags", "");
            args.putIfAbsent("includeCompleted", true);
        }
    }

    private JsonNode parseToolCall(String response) {
        String normalized = stripCodeFences(response);
        try {
            return objectMapper.readTree(normalized);
        } catch (Exception e) {
            throw new IllegalArgumentException("Model output is not valid JSON: " + response, e);
        }
    }

    private String stripCodeFences(String text) {
        if (text == null) {
            return "";
        }
        String trimmed = text.trim();
        if (trimmed.startsWith("```") && trimmed.endsWith("```")) {
            int firstNewline = trimmed.indexOf('\n');
            if (firstNewline > 0) {
                return trimmed.substring(firstNewline + 1, trimmed.length() - 3).trim();
            }
        }
        return trimmed;
    }

    private Map<String, Object> jsonObjectToMap(JsonNode node) {
        Map<String, Object> out = new HashMap<>();
        if (node == null || !node.isObject()) {
            return out;
        }
        Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            out.put(entry.getKey(), toJavaValue(entry.getValue()));
        }
        return out;
    }

    private Object toJavaValue(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isTextual()) {
            return node.asText();
        }
        if (node.isBoolean()) {
            return node.asBoolean();
        }
        if (node.isIntegralNumber()) {
            return node.asLong();
        }
        if (node.isFloatingPointNumber()) {
            return node.asDouble();
        }
        if (node.isArray() || node.isObject()) {
            return node;
        }
        return node.asText();
    }
}
