package mn.mcp.server;

import io.micronaut.mcp.annotations.Tool;
import io.micronaut.mcp.annotations.ToolArg;
import jakarta.inject.Singleton;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Singleton
class TaskMcpTools {
    private final TaskStore taskStore;

    TaskMcpTools(TaskStore taskStore) {
        this.taskStore = taskStore;
    }

    @Tool(name = "create-task", description = "Create a new task")
    TaskView createTask(@ToolArg(name = "title") String title,
                        @ToolArg(name = "description") String description,
                        @ToolArg(name = "priority") String priority,
                        @ToolArg(name = "dueDate", description = "Optional yyyy-MM-dd") String dueDate,
                        @ToolArg(name = "tags", description = "Optional comma-separated tags") String tags) {
        TaskPriority parsedPriority = TaskInputParser.parsePriority(priority).orElse(TaskPriority.MEDIUM);
        LocalDate parsedDueDate = TaskInputParser.parseDate(dueDate).orElse(null);
        return taskStore.create(
            TaskInputParser.nonBlankOrDefault(title, "Untitled task"),
            TaskInputParser.nonBlankOrDefault(description, ""),
            parsedPriority,
            parsedDueDate,
            TaskStore.parseTags(tags)
        );
    }

    @Tool(name = "get-task", description = "Get task details by task ID")
    Object getTask(@ToolArg(name = "id") long id) {
        return taskStore.byId(id).<Object>map(task -> task).orElse("TASK_NOT_FOUND");
    }

    @Tool(name = "list-tasks", description = "List tasks with optional filters")
    List<TaskView> listTasks(@ToolArg(name = "status", description = "Optional: OPEN, IN_PROGRESS, DONE") Optional<String> status,
                             @ToolArg(name = "priority", description = "Optional: LOW, MEDIUM, HIGH") Optional<String> priority,
                             @ToolArg(name = "includeCompleted", description = "Optional true/false, defaults to true") Optional<Boolean> includeCompleted) {
        boolean includeDone = includeCompleted.orElse(true);
        return taskStore.list(
            TaskInputParser.parseStatus(status.orElse(null)),
            TaskInputParser.parsePriority(priority.orElse(null)),
            includeDone
        );
    }

    @Tool(name = "search-tasks", description = "Search tasks by text in title, description, or tags")
    List<TaskView> searchTasks(@ToolArg(name = "query") String query) {
        return taskStore.search(TaskInputParser.nonBlankOrDefault(query, ""));
    }

    @Tool(name = "complete-task", description = "Mark a task as done")
    Object completeTask(@ToolArg(name = "id") long id,
                        @ToolArg(name = "note", description = "Optional completion note") String note) {
        return taskStore.updateStatus(id, TaskStatus.DONE, note).<Object>map(task -> task).orElse("TASK_NOT_FOUND");
    }

    @Tool(name = "start-task", description = "Move a task to IN_PROGRESS")
    Object startTask(@ToolArg(name = "id") long id) {
        return taskStore.updateStatus(id, TaskStatus.IN_PROGRESS, "").<Object>map(task -> task).orElse("TASK_NOT_FOUND");
    }

    @Tool(name = "set-priority", description = "Change task priority")
    Object setPriority(@ToolArg(name = "id") long id,
                       @ToolArg(name = "priority") String priority) {
        Optional<TaskPriority> newPriority = TaskInputParser.parsePriority(priority);
        if (newPriority.isEmpty()) {
            return "INVALID_PRIORITY";
        }
        return taskStore.updatePriority(id, newPriority.get()).<Object>map(task -> task).orElse("TASK_NOT_FOUND");
    }

    @Tool(name = "delete-task", description = "Delete task by task ID")
    String deleteTask(@ToolArg(name = "id") long id) {
        return taskStore.delete(id) ? "DELETED" : "TASK_NOT_FOUND";
    }

    @Tool(name = "task-agent", description = "Simple task agent. command: create|get|list|delete")
    Object taskAgent(@ToolArg(name = "command", description = "create|get|list|delete") Optional<String> command,
                     @ToolArg(name = "id", description = "Task ID for get/delete") Optional<Long> id,
                     @ToolArg(name = "title", description = "Task title for create") Optional<String> title,
                     @ToolArg(name = "description", description = "Optional description for create") Optional<String> description,
                     @ToolArg(name = "priority", description = "Optional LOW|MEDIUM|HIGH for create") Optional<String> priority,
                     @ToolArg(name = "dueDate", description = "Optional yyyy-MM-dd for create") Optional<String> dueDate,
                     @ToolArg(name = "tags", description = "Optional comma-separated tags for create") Optional<String> tags,
                     @ToolArg(name = "includeCompleted", description = "Optional for list, default true") Optional<Boolean> includeCompleted) {
        String normalized = TaskInputParser.nonBlankOrDefault(command.orElse(null), "list").toLowerCase();
        return switch (normalized) {
            case "create" -> createTask(
                title.orElse(""),
                description.orElse(""),
                priority.orElse(""),
                dueDate.orElse(""),
                tags.orElse("")
            );
            case "get" -> id.<Object>map(this::getTask).orElse("MISSING_ID");
            case "delete" -> id.map(this::deleteTask).orElse("MISSING_ID");
            case "list" -> listTasks(Optional.empty(), Optional.empty(), includeCompleted);
            default -> "INVALID_COMMAND";
        };
    }
}
