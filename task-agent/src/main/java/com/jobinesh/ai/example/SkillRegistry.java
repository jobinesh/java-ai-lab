package com.jobinesh.ai.example;

import io.micronaut.core.io.ResourceLoader;
import jakarta.inject.Singleton;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Singleton
class SkillRegistry {
    private static final String SKILLS_RESOURCE = "classpath:skills.md";

    private final ResourceLoader resourceLoader;

    SkillRegistry(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    String skillsMarkdown() {
        try (InputStream in = resourceLoader.getResourceAsStream(SKILLS_RESOURCE)
            .orElseThrow(() -> new IllegalStateException("Missing resource: " + SKILLS_RESOURCE))) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read skills resource: " + SKILLS_RESOURCE, e);
        }
    }
}
