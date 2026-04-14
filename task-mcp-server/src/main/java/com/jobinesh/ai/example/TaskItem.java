package com.jobinesh.ai.example;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

final class TaskItem {
    private final long id;
    private final String title;
    private final String description;
    private final Instant createdAt;

    private TaskStatus status;
    private TaskPriority priority;
    private LocalDate dueDate;
    private List<String> tags;
    private String completionNote;
    private Instant updatedAt;

    TaskItem(long id,
             String title,
             String description,
             TaskPriority priority,
             LocalDate dueDate,
             List<String> tags) {
        Instant now = Instant.now();
        this.id = id;
        this.title = title;
        this.description = description;
        this.priority = priority;
        this.dueDate = dueDate;
        this.tags = tags;
        this.status = TaskStatus.OPEN;
        this.completionNote = "";
        this.createdAt = now;
        this.updatedAt = now;
    }

    long id() {
        return id;
    }

    String title() {
        return title;
    }

    String description() {
        return description;
    }

    TaskStatus status() {
        return status;
    }

    TaskPriority priority() {
        return priority;
    }

    LocalDate dueDate() {
        return dueDate;
    }

    List<String> tags() {
        return tags;
    }

    String completionNote() {
        return completionNote;
    }

    Instant createdAt() {
        return createdAt;
    }

    Instant updatedAt() {
        return updatedAt;
    }

    void updateStatus(TaskStatus newStatus, String note) {
        this.status = newStatus;
        if (note != null && !note.isBlank()) {
            this.completionNote = note;
        }
        this.updatedAt = Instant.now();
    }

    void updatePriority(TaskPriority newPriority) {
        this.priority = newPriority;
        this.updatedAt = Instant.now();
    }

    TaskView toView() {
        return new TaskView(
            id,
            title,
            description,
            status,
            priority,
            dueDate,
            List.copyOf(tags),
            completionNote,
            createdAt,
            updatedAt
        );
    }
}
