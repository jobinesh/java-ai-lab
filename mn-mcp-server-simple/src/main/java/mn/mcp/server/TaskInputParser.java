package mn.mcp.server;

import java.time.LocalDate;
import java.util.Optional;

final class TaskInputParser {
    private TaskInputParser() {
    }

    static Optional<TaskPriority> parsePriority(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(TaskPriority.valueOf(raw.trim().toUpperCase()));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    static Optional<TaskStatus> parseStatus(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(TaskStatus.valueOf(raw.trim().toUpperCase()));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    static Optional<LocalDate> parseDate(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(LocalDate.parse(raw.trim()));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    static String nonBlankOrDefault(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }
}
