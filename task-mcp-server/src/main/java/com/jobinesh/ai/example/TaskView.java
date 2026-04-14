package com.jobinesh.ai.example;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

record TaskView(long id,
                String title,
                String description,
                TaskStatus status,
                TaskPriority priority,
                LocalDate dueDate,
                List<String> tags,
                String completionNote,
                Instant createdAt,
                Instant updatedAt) {
}
