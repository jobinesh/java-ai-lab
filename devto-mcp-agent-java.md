---
title: "Build a Simple AI Agent in Java with MCP (Micronaut + LangChain4j)"
published: false
description: "A practical, simple guide to AI agents and why MCP matters, with a working Java example."
tags: java, ai, mcp, langchain4j
---

AI agents are often explained in abstract terms. Let’s keep it practical.

In this project, we build a small **task-management agent** in Java. The agent reads a natural-language instruction, decides which tool to call, and executes that tool through an MCP server.

## What is an AI agent?

At a minimum, an agent is:

1. A model that can reason over an instruction.
2. A set of tools it can use.
3. An execution loop: decide -> call tool -> return result.

In this repo, the loop is handled by `task-agent`:

- `TaskPlannerAiService` (LangChain4j) asks the model to output one JSON tool call.
- `TaskAgentOrchestrator` parses that JSON.
- `McpTaskClient` calls the selected MCP tool.

So the model is not directly changing business data. It chooses an action, and the server executes it safely.

## Why MCP matters

Without MCP, every app invents a custom tool API format. That creates fragile integrations.

**MCP (Model Context Protocol)** gives a standard way to expose tools and call them over JSON-RPC.

In this project, MCP provides:

- A stable tool surface (`create-task`, `list-tasks`, `complete-task`, etc.).
- Structured arguments via tool schemas.
- A predictable call flow (`initialize` then `tools/call`).
- Decoupling: the agent runtime and tool server can evolve independently.

In short: MCP is the contract between “LLM decision-making” and “real system actions.”

## Project architecture

This repo has two modules:

- `task-mcp-server`: exposes task tools via MCP and REST.
- `task-agent`: runs an LLM-backed planner and invokes MCP tools.

### 1) MCP tool server (`task-mcp-server`)

`TaskMcpTools` defines tools with Micronaut MCP annotations:

- `@Tool(name = "create-task")`
- `@Tool(name = "list-tasks")`
- `@Tool(name = "complete-task")`
- `@Tool(name = "set-priority")`
- etc.

These tools operate on an in-memory `TaskStore`.

Important detail: both REST and MCP use the same store. So data created by REST is immediately visible to MCP, and vice versa.

### 2) Agent module (`task-agent`)

The agent behavior is constrained by `src/main/resources/skills.md`.

`TaskPlannerAiService` loads this file directly with LangChain4j:

```java
@SystemMessage(fromResource = "skills.md")
@UserMessage("User instruction: {{instruction}}")
String plan(@V("instruction") String instruction);
```

That means your tool policy is editable in Markdown instead of hardcoded Java strings.

Then `TaskAgentOrchestrator`:

- reads model output,
- validates/parses JSON,
- applies safe defaults for missing args,
- calls MCP via `McpTaskClient`.

`McpTaskClient` performs JSON-RPC calls to `http://127.0.0.1:8080/mcp`, starting with `initialize` and then `tools/call`.

## End-to-end flow

For instruction:

> "Create task Buy milk with high priority and tag home"

Flow is:

1. Agent sends instruction + skills contract to the model.
2. Model returns JSON, for example:

```json
{"tool":"create-task","arguments":{"title":"Buy milk","priority":"HIGH","tags":"home"}}
```

3. Orchestrator parses JSON and fills optional defaults.
4. MCP client calls `create-task` on the task server.
5. Result is returned to the caller.

This pattern is small but production-relevant: planner decisions are separated from tool execution.

## Why this pattern scales

This approach scales better than a single monolithic prompt:

- You can add new tools without redesigning agent logic.
- You can tighten guardrails in `skills.md`.
- You can swap model/provider with minimal business logic changes.
- You keep execution in deterministic server code, not in free-form model output.

## Run it locally

Start MCP server:

```bash
cd task-mcp-server
mvn exec:java
```

Start agent:

```bash
cd task-agent
OPENAI_API_KEY=<your-key> mvn exec:java
```

Call agent endpoint:

```bash
curl -sS -X POST http://127.0.0.1:8081/api/agent/run \
  -H 'content-type: application/json' \
  -d '{"instruction":"Create task Buy milk with high priority and tag home"}'
```

Inspect skills contract:

```bash
curl -sS http://127.0.0.1:8081/api/agent/skills
```

## Final takeaway

Think of roles like this:

- **Agent (LangChain4j):** decides *what to do*.
- **MCP server:** defines *what can be done* and executes it.
- **Your business code:** controls *how it is done safely*.

That separation is the key. It keeps your AI layer flexible and your system behavior reliable.
