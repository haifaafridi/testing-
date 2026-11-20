package taskflow;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.channels.FileChannel;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.StandardCopyOption.ATOMIC_MOVE;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.WRITE;

/**
 * Stores one workspace as UTF-8 properties and locks it for the editing session.
 * @author TaskFlow project
 */
public final class TaskStore implements AutoCloseable {
    private final Path directory;
    private final Path file;
    private final FileChannel lockChannel;

    /**
     * Opens a workspace directory and obtains its single-writer lock.
     * @param directory workspace directory, created on first use
     * @throws IOException if it cannot be opened or another instance is using it
     */
    public TaskStore(Path directory) throws IOException {
        this.directory = directory.toAbsolutePath();
        file = this.directory.resolve("tasks.properties");
        Files.createDirectories(this.directory);
        lockChannel = FileChannel.open(this.directory.resolve("workspace.lock"), CREATE, WRITE);
        try {
            if (lockChannel.tryLock() == null) throw new IOException("This workspace is already open.");
        } catch (OverlappingFileLockException e) {
            lockChannel.close();
            throw new IOException("This workspace is already open.", e);
        } catch (IOException e) {
            lockChannel.close();
            throw e;
        }
    }

    /**
     * Reads and validates every record before exposing any tasks.
     * @return tasks in creation order, or an empty list on first use
     * @throws IOException if the file is unreadable or contains invalid records
     */
    public List<Task> read() throws IOException {
        List<Task> rows = new ArrayList<>();
        if (Files.notExists(file)) return rows;
        Properties data = new Properties();
        try (Reader input = Files.newBufferedReader(file, UTF_8)) {
            data.load(input);
            if (!"1".equals(data.getProperty("version"))) throw new IllegalArgumentException();
            int count = Integer.parseInt(data.getProperty("count", ""));
            if (count < 0 || data.size() != 2L + 4L * count) throw new IllegalArgumentException();
            Set<UUID> seen = new HashSet<>();
            for (int i = 0; i < count; i++) {
                String prefix = i + ".";
                UUID id = UUID.fromString(data.getProperty(prefix + "id", ""));
                if (!seen.add(id)) throw new IllegalArgumentException();
                String notes = data.getProperty(prefix + "description");
                if (notes == null) throw new IllegalArgumentException();
                rows.add(new Task(id, data.getProperty(prefix + "title"), notes,
                        Status.valueOf(data.getProperty(prefix + "status", ""))));
            }
        } catch (IllegalArgumentException e) {
            throw new IOException("The saved task file is invalid. Original file: " + file, e);
        }
        return rows;
    }

    /**
     * Writes all tasks to a temporary sibling, then atomically replaces the saved file.
     * @param tasks complete new snapshot
     * @throws IOException if saving fails
     */
    public void write(List<Task> tasks) throws IOException {
        Properties data = new Properties();
        data.setProperty("version", "1");
        data.setProperty("count", Integer.toString(tasks.size()));
        for (int i = 0; i < tasks.size(); i++) {
            Task t = tasks.get(i);
            String prefix = i + ".";
            data.setProperty(prefix + "id", t.id().toString());
            data.setProperty(prefix + "title", t.title());
            data.setProperty(prefix + "description", t.description());
            data.setProperty(prefix + "status", t.status().name());
        }
        Path temp = Files.createTempFile(directory, "tasks-", ".tmp");
        try {
            try (Writer output = Files.newBufferedWriter(temp, UTF_8)) {
                data.store(output, "TaskFlow workspace");
            }
            Files.move(temp, file, ATOMIC_MOVE, REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temp);
        }
    }

    /** Releases the session lock, allowing the workspace to reopen.
     * @throws IOException if the channel cannot be closed
     */
    @Override public void close() throws IOException {
        lockChannel.close();
    }
}
