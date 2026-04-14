# task-mcp-server

A meaningful Micronaut MCP server example: an in-memory task tracker.

## What it does

It exposes MCP tools for daily task management:

- `create-task`
- `get-task`
- `list-tasks`
- `search-tasks`
- `start-task`
- `complete-task`
- `set-priority`
- `delete-task`
- `task-agent` (simple command-based task agent)

Separate LLM agent module is now in `task-agent/` (own Maven module and runtime).

## Build

```bash
cd /Users/jmpurush/mywork/ai-llm/mcp/java-ai-lab/task-mcp-server
mvn clean package -DskipTests
```

## Run

```bash
mvn exec:java
```

Server endpoint: `http://localhost:8080/mcp`

REST endpoint base: `http://localhost:8080/api/tasks`

## Quick MCP test via curl

Initialize:

```bash
curl -sS -H 'content-type: application/json' \
  -d '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2024-11-05","capabilities":{},"clientInfo":{"name":"local-test","version":"1.0"}}}' \
  http://127.0.0.1:8080/mcp
```

Create task:

```bash
curl -sS -H 'content-type: application/json' \
  -d '{"jsonrpc":"2.0","id":2,"method":"tools/call","params":{"name":"create-task","arguments":{"title":"Prepare monthly report","description":"Collect billing data and draft summary","priority":"HIGH","dueDate":"2026-04-20","tags":"finance,reporting"}}}' \
  http://127.0.0.1:8080/mcp
```

List tasks:

```bash
curl -sS -H 'content-type: application/json' \
  -d '{"jsonrpc":"2.0","id":3,"method":"tools/call","params":{"name":"list-tasks","arguments":{"includeCompleted":true}}}' \
  http://127.0.0.1:8080/mcp
```

Complete task (replace ID):

```bash
curl -sS -H 'content-type: application/json' \
  -d '{"jsonrpc":"2.0","id":4,"method":"tools/call","params":{"name":"complete-task","arguments":{"id":1001,"note":"Submitted to finance lead"}}}' \
  http://127.0.0.1:8080/mcp
```

Task agent create:

```bash
curl -sS -H 'content-type: application/json' \
  -d '{"jsonrpc":"2.0","id":5,"method":"tools/call","params":{"name":"task-agent","arguments":{"command":"create","title":"Plan sprint","description":"Prepare sprint backlog","priority":"MEDIUM","tags":"planning,sprint"}}}' \
  http://127.0.0.1:8080/mcp
```

Task agent list:

```bash
curl -sS -H 'content-type: application/json' \
  -d '{"jsonrpc":"2.0","id":6,"method":"tools/call","params":{"name":"task-agent","arguments":{"command":"list","includeCompleted":true}}}' \
  http://127.0.0.1:8080/mcp
```

See `task-agent/README.md` for LLM agent APIs and usage.

## REST add/delete using same in-memory store

Create task by REST:

```bash
curl -sS -X POST http://127.0.0.1:8080/api/tasks \
  -H 'content-type: application/json' \
  -d '{"title":"Buy groceries","description":"milk and bread","priority":"HIGH","dueDate":"2026-04-21","tags":"home,errand"}'
```

Delete task by REST:

```bash
curl -sS -X DELETE http://127.0.0.1:8080/api/tasks/1001
```

Because REST and MCP both use the same `TaskStore` singleton, tasks created via REST are immediately visible in MCP `list-tasks` and `get-task`.
