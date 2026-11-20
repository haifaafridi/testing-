package taskflow;

import java.util.Objects;
import java.util.UUID;

/**
 * An immutable task. Edits keep the same identifier, even if the title changes.
 * @param id stable identifier
 * @param title required task title
 * @param description optional free-form notes
 * @param status current stage of work
 * @author TaskFlow project
 */
public record Task(UUID id, String title, String description, Status status) {
    /**
     * Validates the title and removes surrounding whitespace from text fields.
     * @param id stable identifier
     * @param title required task title
     * @param description optional notes, or null for no notes
     * @param status current stage
     * @throws IllegalArgumentException if the title is empty or the status is missing
     * @throws NullPointerException if the identifier is null
     */
    public Task {
        Objects.requireNonNull(id, "A task identifier is required.");
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Enter a task title.");
        }
        if (status == null) {
            throw new IllegalArgumentException("Choose a task status.");
        }
        title = title.strip();
        description = description == null ? "" : description.strip();
    }
}
