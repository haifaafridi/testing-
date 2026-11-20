package taskflow;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

/**
 * Executable checks for task identity, validation and storage in temporary workspaces.
 * @author TaskFlow project
 */
public final class TaskChecks {
    private TaskChecks() { }

    /** Runs all model and storage checks.
     * @param args unused
     * @throws Exception if a check or file operation fails
     */
    public static void main(String[] args) throws Exception {
        Path root = Files.createTempDirectory("taskflow-checks-");
        try {
            Path work = root.resolve("main");
            String notes = "فہرست\nA = B: C\\D";
            Task first;
            try (TaskStore store = new TaskStore(work)) {
                TaskManager manager = new TaskManager(store);
                UUID missing = UUID.randomUUID();
                try {
                    manager.update(missing, "Sample", "", Status.PENDING);
                    throw new AssertionError("Missing update accepted");
                } catch (NoSuchElementException e) {
                    check("M01", manager.all().isEmpty(), e.getMessage() + " Count = " + manager.all().size());
                }
                try {
                    manager.delete(missing);
                    throw new AssertionError("Missing deletion accepted");
                } catch (NoSuchElementException e) {
                    check("M02", manager.all().isEmpty(), e.getMessage() + " Count = " + manager.all().size());
                }
                first = manager.add("  Notes  ", notes, Status.PENDING);
                Task second = manager.add("Notes", null, Status.COMPLETED);
                manager.delete(second.id());
                check("M03", manager.all().equals(List.of(first)) && first.title().equals("Notes"),
                        "Duplicate removed by ID. Remaining title = " + manager.all().get(0).title());
                try {
                    manager.update(first.id(), " \t ", "Changed", Status.COMPLETED);
                    throw new AssertionError("Blank update accepted");
                } catch (IllegalArgumentException e) {
                    check("M04", manager.all().equals(List.of(first)), e.getMessage() + " Original task retained.");
                }
                try {
                    manager.add("Test", "", null);
                    throw new AssertionError("Missing status accepted");
                } catch (IllegalArgumentException e) {
                    check("M05", manager.all().size() == 1, e.getMessage() + " Count = " + manager.all().size());
                }
                try (TaskStore secondStore = new TaskStore(work)) {
                    throw new AssertionError("Second writer admitted: " + secondStore);
                } catch (IOException e) {
                    check("M06", e.getMessage().contains("already open"), e.getMessage());
                }
            }
            try (TaskStore store = new TaskStore(work)) {
                Task restored = new TaskManager(store).all().get(0);
                check("M07", restored.equals(first) && notes.equals(restored.description()),
                        "Same ID, status and UTF-8 multiline text after reopen.");
            }
            Path blocked = root.resolve("blocked");
            try (TaskStore store = new TaskStore(blocked)) {
                TaskManager manager = new TaskManager(store);
                Path target = Files.createDirectory(blocked.resolve("tasks.properties"));
                Files.writeString(target.resolve("obstruction"), "test only");
                try {
                    manager.add("Will fail", "", Status.PENDING);
                    throw new AssertionError("Unexpected successful write");
                } catch (IOException e) {
                    check("M08", manager.all().isEmpty(), "Save refused. In-memory count = " + manager.all().size());
                }
                Files.delete(target.resolve("obstruction"));
                Files.delete(target);
                manager.add("Retry", "", Status.PENDING);
                check("M09", store.read().equals(manager.all()), "Retry saved " + store.read().size() + " task.");
            }
            Path corrupt = Files.createDirectories(root.resolve("corrupt"));
            Path file = corrupt.resolve("tasks.properties");
            String invalid = "version=1\ncount=1\n0.id=broken\n";
            Files.writeString(file, invalid);
            try (TaskStore store = new TaskStore(corrupt)) {
                try {
                    new TaskManager(store);
                    throw new AssertionError("Invalid record loaded");
                } catch (IOException e) {
                    check("M10", Files.readString(file).equals(invalid), "Invalid file reported. Original bytes retained.");
                }
            }
            System.out.println("10/10 model and storage checks passed.");
        } finally {
            try (var paths = Files.walk(root)) {
                for (Path p : paths.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(p);
            }
        }
    }

    /** Records observed output after verifying the condition. */
    private static void check(String id, boolean passed, String observed) {
        if (!passed) throw new AssertionError(id + " | FAIL | " + observed);
        System.out.println(id + " | PASS | " + observed);
    }
}
