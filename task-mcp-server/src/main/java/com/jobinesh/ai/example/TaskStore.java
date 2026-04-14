package com.jobinesh.ai.example;

import jakarta.inject.Singleton;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Singleton
class TaskStore {
    private final AtomicLong idGenerator = new AtomicLong(1000);
    private final Map<Long, TaskItem> tasks = new ConcurrentHashMap<>();

    TaskView create(String title,
                    String description,
                    TaskPriority priority,
                    LocalDate dueDate,
                    List<String> tags) {
        long id = idGenerator.incrementAndGet();
        TaskItem task = new TaskItem(id, title, description, priority, dueDate, tags);
        tasks.put(id, task);
        return task.toView();
    }

    Optional<TaskView> byId(long id) {
        return Optional.ofNullable(tasks.get(id)).map(TaskItem::toView);
    }

    List<TaskView> list(Optional<TaskStatus> statusFilter,
                        Optional<TaskPriority> priorityFilter,
                        boolean includeCompleted) {
        return tasks.values().stream()
            .filter(task -> includeCompleted || task.status() != TaskStatus.DONE)
            .filter(task -> statusFilter.map(s -> task.status() == s).orElse(true))
            .filter(task -> priorityFilter.map(p -> task.priority() == p).orElse(true))
            .sorted(Comparator.comparing(TaskItem::id))
            .map(TaskItem::toView)
            .toList();
    }

    List<TaskView> search(String query) {
        String normalized = query == null ? "" : query.toLowerCase(Locale.ROOT);
        return tasks.values().stream()
            .filter(task -> contains(task.title(), normalized)
                || contains(task.description(), normalized)
                || task.tags().stream().anyMatch(tag -> contains(tag, normalized)))
            .sorted(Comparator.comparing(TaskItem::id))
            .map(TaskItem::toView)
            .toList();
    }

    Optional<TaskView> updateStatus(long id, TaskStatus newStatus, String note) {
        TaskItem task = tasks.get(id);
        if (task == null) {
            return Optional.empty();
        }
        task.updateStatus(newStatus, note);
        return Optional.of(task.toView());
    }

    Optional<TaskView> updatePriority(long id, TaskPriority newPriority) {
        TaskItem task = tasks.get(id);
        if (task == null) {
            return Optional.empty();
        }
        task.updatePriority(newPriority);
        return Optional.of(task.toView());
    }

    boolean delete(long id) {
        return tasks.remove(id) != null;
    }

    private static boolean contains(String text, String part) {
        return text != null && text.toLowerCase(Locale.ROOT).contains(part);
    }

    static List<String> parseTags(String tagsCsv) {
        if (tagsCsv == null || tagsCsv.isBlank()) {
            return List.of();
        }
        String[] raw = tagsCsv.split(",");
        List<String> tags = new ArrayList<>();
        for (String tag : raw) {
            String cleaned = tag.trim();
            if (!cleaned.isEmpty()) {
                tags.add(cleaned);
            }
        }
        return List.copyOf(tags);
    }
}
