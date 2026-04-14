package com.jobinesh.ai.example;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Delete;
import io.micronaut.http.annotation.Post;

@Controller("/api/tasks")
class TaskRestApiController {
    private final TaskStore taskStore;

    TaskRestApiController(TaskStore taskStore) {
        this.taskStore = taskStore;
    }

    @Post
    HttpResponse<?> createTask(@Body CreateTaskRequest request) {
        if (request == null) {
            return HttpResponse.badRequest(new ErrorResponse("Request body is required"));
        }

        TaskPriority parsedPriority = TaskInputParser.parsePriority(request.priority()).orElse(TaskPriority.MEDIUM);
        TaskView created = taskStore.create(
            TaskInputParser.nonBlankOrDefault(request.title(), "Untitled task"),
            TaskInputParser.nonBlankOrDefault(request.description(), ""),
            parsedPriority,
            TaskInputParser.parseDate(request.dueDate()).orElse(null),
            TaskStore.parseTags(request.tags())
        );
        return HttpResponse.created(created);
    }

    @Delete("/{id}")
    HttpResponse<?> deleteTask(long id) {
        boolean deleted = taskStore.delete(id);
        if (!deleted) {
            return HttpResponse.notFound(new ErrorResponse("TASK_NOT_FOUND"));
        }
        return HttpResponse.ok(new DeleteResponse("DELETED", id));
    }

    record CreateTaskRequest(String title,
                             String description,
                             String priority,
                             String dueDate,
                             String tags) {
    }

    record DeleteResponse(String status, long id) {
    }

    record ErrorResponse(String message) {
    }
}
