---
title: Build a Meaningful MCP Server with Micronaut (Step-by-Step)
published: false
description: Learn MCP basics and build a practical in-memory task tracker server using Micronaut MCP annotations.
tags: java, micronaut, mcp, ai
---

# Build a Meaningful MCP Server with Micronaut (Step-by-Step)

If you are building AI systems that need to *do* things (not just chat), MCP (Model Context Protocol) is one of the cleanest ways to expose capabilities.

In this tutorial, you will build a practical MCP server using **Micronaut** and Java: an in-memory **task tracker** with tools like `create-task`, `list-tasks`, `complete-task`, and `search-tasks`.

## What is an MCP server?

An MCP server is a process that exposes structured capabilities (tools, prompts, resources) to an MCP client (for example, an AI assistant host).

At a high level:

1. The client connects to your server.
2. The client asks what tools are available.
3. The client calls a tool with JSON arguments.
4. Your server runs business logic and returns structured output.

So instead of giving an LLM direct access to your app internals, you expose controlled operations through MCP.

## Why Micronaut for MCP?

Micronaut gives you:

- Fast startup and low overhead.
- Compile-time DI and introspection.
- Annotation-based MCP tool registration.

The key annotation support you use in this example:

- `@Singleton`: Declares a bean managed by Micronaut.
- `@Tool`: Exposes a method as an MCP tool.
- `@ToolArg`: Names and documents tool arguments for MCP schema generation.

That means you can implement your domain logic in normal Java classes and expose it as MCP with minimal boilerplate.

## Project used in this article

This article is based on the sample project here:

`/Users/jmpurush/mywork/ai-llm/mcp/mn-mcp-server-simple`

It currently uses:

- Micronaut: `4.10.8-oracle-00001`
- Micronaut MCP server SDK: `0.0.20`

## 1) Create the Maven project

`pom.xml` uses Micronaut BOM + MCP SDK + Netty HTTP server + Jackson:

```xml
<dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>io.micronaut</groupId>
      <artifactId>micronaut-core-bom</artifactId>
      <version>4.10.8-oracle-00001</version>
      <type>pom</type>
      <scope>import</scope>
    </dependency>
  </dependencies>
</dependencyManagement>

<dependencies>
  <dependency>
    <groupId>io.micronaut</groupId>
    <artifactId>micronaut-runtime</artifactId>
  </dependency>
  <dependency>
    <groupId>io.micronaut</groupId>
    <artifactId>micronaut-jackson-databind</artifactId>
  </dependency>
  <dependency>
    <groupId>io.micronaut</groupId>
    <artifactId>micronaut-http-server-netty</artifactId>
  </dependency>
  <dependency>
    <groupId>io.micronaut.mcp</groupId>
    <artifactId>micronaut-mcp-server-java-sdk</artifactId>
    <version>0.0.20</version>
  </dependency>
</dependencies>
```

## 2) Configure MCP server transport

In `src/main/resources/application.properties`:

```properties
micronaut.application.name=mn-mcp-server-simple
micronaut.server.port=8080

micronaut.mcp.server.transport=HTTP
micronaut.mcp.server.info.name=Simple In-Memory MCP Server
micronaut.mcp.server.info.version=0.1.0
```

This exposes MCP over HTTP at `/mcp`.

## 3) Build a meaningful domain model

Instead of a toy echo tool, we model tasks:

- `TaskItem` (internal mutable model)
- `TaskView` (response DTO)
- `TaskPriority` (`LOW`, `MEDIUM`, `HIGH`)
- `TaskStatus` (`OPEN`, `IN_PROGRESS`, `DONE`)

The in-memory store lives in `TaskStore` (`@Singleton`) and handles create/list/search/update/delete.

## 4) Expose MCP tools with annotations

`TaskTools` is where Micronaut MCP shines.

```java
@Singleton
class TaskTools {
    private final TaskStore taskStore;

    TaskTools(TaskStore taskStore) {
        this.taskStore = taskStore;
    }

    @Tool(name = "create-task", description = "Create a new task")
    TaskView createTask(
        @ToolArg(name = "title") String title,
        @ToolArg(name = "description") String description,
        @ToolArg(name = "priority") String priority,
        @ToolArg(name = "dueDate", description = "Optional yyyy-MM-dd") String dueDate,
        @ToolArg(name = "tags", description = "Optional comma-separated tags") String tags
    ) {
        // parse + validation + store call
    }
}
```

Each method becomes an MCP tool automatically.

In this sample, available tools are:

- `create-task`
- `get-task`
- `list-tasks`
- `search-tasks`
- `start-task`
- `complete-task`
- `set-priority`
- `delete-task`

## 5) Build and run

```bash
cd /Users/jmpurush/mywork/ai-llm/mcp/mn-mcp-server-simple
mvn clean package -DskipTests
mvn exec:java
```

You should see startup log similar to:

```text
Startup completed ... Server Running: http://localhost:8080
```

## 6) Test the MCP server manually with curl

### Initialize

```bash
curl -sS -H 'content-type: application/json' \
  -d '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2024-11-05","capabilities":{},"clientInfo":{"name":"local-test","version":"1.0"}}}' \
  http://127.0.0.1:8080/mcp
```

### Create a task

```bash
curl -sS -H 'content-type: application/json' \
  -d '{"jsonrpc":"2.0","id":2,"method":"tools/call","params":{"name":"create-task","arguments":{"title":"Prepare monthly report","description":"Collect billing data and draft summary","priority":"HIGH","dueDate":"2026-04-20","tags":"finance,reporting"}}}' \
  http://127.0.0.1:8080/mcp
```

### List tasks

```bash
curl -sS -H 'content-type: application/json' \
  -d '{"jsonrpc":"2.0","id":3,"method":"tools/call","params":{"name":"list-tasks","arguments":{"status":"","priority":"","includeCompleted":true}}}' \
  http://127.0.0.1:8080/mcp
```

### Complete a task

```bash
curl -sS -H 'content-type: application/json' \
  -d '{"jsonrpc":"2.0","id":4,"method":"tools/call","params":{"name":"complete-task","arguments":{"id":1001,"note":"Submitted to finance lead"}}}' \
  http://127.0.0.1:8080/mcp
```

## How Micronaut maps this to MCP

When your server starts:

1. Micronaut scans `@Singleton` beans.
2. MCP integration finds methods annotated with `@Tool`.
3. Argument schema is inferred from `@ToolArg` + method signatures.
4. Tools are exposed through MCP endpoint `/mcp`.

So your Java code stays focused on domain behavior while Micronaut handles registration and invocation plumbing.

## Practical next steps

To make this production-ready, you can:

- Persist tasks in a database instead of memory.
- Add auth and tenant scoping.
- Add `@Resource` endpoints for task snapshots.
- Add richer validation and error models.
- Add unit and integration tests for each tool.

## Final thoughts

MCP becomes much more useful when tools represent real business actions.

Micronaut makes that straightforward: write regular Java service code, annotate tool methods, and ship an MCP server quickly.

If you want, I can also generate a follow-up article for:

- `@Resource` and `@Prompt` support in Micronaut MCP
- Adding persistence (PostgreSQL) to this task tracker
- Testing MCP tools with JUnit and integration tests
