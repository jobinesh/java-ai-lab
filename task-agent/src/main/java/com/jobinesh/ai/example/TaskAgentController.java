package com.jobinesh.ai.example;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.Produces;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;

import java.util.Map;

@Controller("/api/agent")
@ExecuteOn(TaskExecutors.BLOCKING)
class TaskAgentController {
    private final SkillRegistry skillRegistry;
    private final TaskAgentOrchestrator orchestrator;

    TaskAgentController(SkillRegistry skillRegistry, TaskAgentOrchestrator orchestrator) {
        this.skillRegistry = skillRegistry;
        this.orchestrator = orchestrator;
    }

    @Get(uri = "/skills", produces = MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    String skills() {
        return skillRegistry.skillsMarkdown();
    }

    @Post("/run")
    HttpResponse<?> run(@Body RunRequest request) {
        if (request == null || request.instruction() == null || request.instruction().isBlank()) {
            return HttpResponse.badRequest(Map.of("error", "instruction is required"));
        }
        return HttpResponse.ok(orchestrator.runInstruction(request.instruction()));
    }

    record RunRequest(String instruction) {
    }
}
