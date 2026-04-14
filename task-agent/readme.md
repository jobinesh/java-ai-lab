# task-agent

Separate agent module with Micronaut LangChain4j.

This service runs on port `8081` and uses:
- Micronaut LangChain4j (`@AiService`) with OpenAI chat model
- Task MCP server API (`http://127.0.0.1:8080/mcp`) to execute selected task tool
- `src/main/resources/skills.md` as the agent skill contract loaded via LangChain4j `@SystemMessage(fromResource = "skills.md")`

## Run

Start task MCP server first (main module):
```bash
mvn exec:java
```

Start agent module in another terminal:
```bash
cd task-agent
OPENAI_API_KEY=<your-key> mvn exec:java
```

## Endpoints

Get skills:
```bash
curl -sS http://127.0.0.1:8081/api/agent/skills
```

Run agent instruction:
```bash
curl -sS -X POST http://127.0.0.1:8081/api/agent/run \
  -H 'content-type: application/json' \
  -d '{"instruction":"Create task Buy milk with high priority and tag home"}'
```

Response includes:
- selected MCP tool
- tool arguments
- MCP result
- raw model output
