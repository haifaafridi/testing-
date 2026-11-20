package taskflow;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

/**
 * Maintains tasks in creation order and applies changes using stable identifiers.
 * @author TaskFlow project
 */
public final class TaskManager {
    private final TaskStore store;
    private List<Task> tasks;

    /** Loads a workspace for editing.
     * @param store open storage for this session
     * @throws IOException if its saved tasks cannot be read
     */
    public TaskManager(TaskStore store) throws IOException {
        this.store = store;
        tasks = store.read();
    }

    /** Returns the tasks in creation order.
     * @return a read-only snapshot
     */
    public List<Task> all() {
        return List.copyOf(tasks);
    }

    /** Creates a task.
     * @param title required title
     * @param description optional notes
     * @param status initial stage
     * @throws IOException if the change cannot be saved
     * @return the new task
     */
    public Task add(String title, String description, Status status) throws IOException {
        Task task = new Task(UUID.randomUUID(), title, description, status);
        List<Task> next = new ArrayList<>(tasks);
        next.add(task);
        save(next);
        return task;
    }

    /** Replaces a task in its original list position.
     * @param id original identifier
     * @param title revised title
     * @param description revised notes
     * @param status revised stage
     * @return the replacement task
     * @throws IOException if the change cannot be saved
     * @throws NoSuchElementException if the task is missing
     */
    public Task update(UUID id, String title, String description, Status status) throws IOException {
        int position = indexOf(id);
        Task replacement = new Task(id, title, description, status);
        List<Task> next = new ArrayList<>(tasks);
        next.set(position, replacement);
        save(next);
        return replacement;
    }

    /** Removes the task with the supplied identifier.
     * @param id task to remove
     * @throws IOException if the change cannot be saved
     * @throws NoSuchElementException if the task is missing
     */
    public void delete(UUID id) throws IOException {
        int position = indexOf(id);
        List<Task> next = new ArrayList<>(tasks);
        next.remove(position);
        save(next);
    }

    /** Publishes a change in memory only after disk storage succeeds. */
    private void save(List<Task> next) throws IOException {
        store.write(next);
        tasks = next;
    }

    /** Finds the current list position for an identifier. */
    private int indexOf(UUID id) {
        for (int i = 0; i < tasks.size(); i++) {
            if (tasks.get(i).id().equals(id)) return i;
        }
        throw new NoSuchElementException("This task no longer exists.");
    }
}
