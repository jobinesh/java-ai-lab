package com.jobinesh.ai.example;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import io.micronaut.langchain4j.annotation.AiService;

@AiService
interface TaskPlannerAiService {

    @SystemMessage(fromResource = "skills.md")
    @UserMessage("User instruction: {{instruction}}")
    String plan(@V("instruction") String instruction);
}
