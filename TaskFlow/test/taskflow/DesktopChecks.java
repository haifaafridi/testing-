package taskflow;

import java.awt.*;
import java.awt.event.WindowEvent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.text.JTextComponent;

/**
 * Drives the actual application window on the Swing event thread and can capture its display.
 * @author TaskFlow project
 */
public final class DesktopChecks {
    private DesktopChecks() { }

    /** Runs the desktop workflow in an isolated temporary workspace.
     * @param args optional directory for screenshots
     * @throws Exception if an interaction or assertion fails
     */
    public static void main(String[] args) throws Exception {
        if (GraphicsEnvironment.isHeadless()) throw new IllegalStateException("Run this check in a graphical session.");
        Path data = Files.createTempDirectory("taskflow-desktop-");
        Path images = args.length == 0 ? null : Files.createDirectories(Path.of(args[0]));
        System.setProperty("taskflow.dir", data.toString());
        try {
            TaskFlow.main(new String[0]);
            JFrame frame = (JFrame) window("TaskFlow — Task Management System");
            JTable table = named(frame, "tasks", JTable.class);
            boolean empty = ui(() -> visibleLabel(frame, "No tasks yet")
                    && !named(frame, "edit", JButton.class).isEnabled());
            check("D01", empty && rows(table) == 0, "No tasks yet. Edit disabled. Rows = " + rows(table));
            capture(images, "01-empty", frame);
            JDialog add = open(frame, "add", "Add task");
            click(add, "save");
            String message = ui(() -> named(add, "form-error", JLabel.class).getText());
            check("D02", add.isShowing() && message.equals("Enter a task title.") && rows(table) == 0, message);
            capture(images, "02-validation", add);
            fill(add, "Prepare the OEL report", "Check the testing table and number the figures.", Status.PENDING);
            capture(images, "03-add", add);
            click(add, "save");
            check("D03", rows(table) == 1 && task(table, 0).status() == Status.PENDING,
                    task(table, 0).title() + " / " + task(table, 0).status());
            addTask(frame, "Review class diagram", "Trace the path from the form to disk.", Status.IN_PROGRESS);
            addTask(frame, "Generate Javadoc", "Open the API index and review the TaskManager methods.", Status.COMPLETED);
            check("D04", rows(table) == 3 && task(table, 0).title().equals("Prepare the OEL report")
                    && task(table, 1).title().equals("Review class diagram")
                    && task(table, 2).title().equals("Generate Javadoc"), "All " + rows(table) + " titles in creation order.");
            ui(() -> { table.clearSelection(); return null; });
            capture(images, "04-workspace", frame);
            select(table, 0);
            Task before = task(table, 0);
            JDialog edit = open(frame, "edit", "Edit task");
            fill(edit, "Print the OEL report", "Check both member IDs before printing.", Status.IN_PROGRESS);
            capture(images, "05-edit", edit);
            click(edit, "save");
            Task revised = task(table, 0);
            check("D05", revised.id().equals(before.id()) && revised.title().equals("Print the OEL report")
                    && revised.description().equals("Check both member IDs before printing.")
                    && revised.status() == Status.IN_PROGRESS, revised.title() + " / " + revised.status() + ". Same ID.");
            edit = open(frame, "edit", "Edit task");
            fill(edit, revised.title(), revised.description(), Status.COMPLETED);
            click(edit, "save");
            click(frame, "filter-COMPLETED");
            check("D06", rows(table) == 2 && task(table, 0).status() == Status.COMPLETED
                    && task(table, 1).status() == Status.COMPLETED, "Completed filter shows " + rows(table) + " completed tasks.");
            capture(images, "06-status", frame);
            setText(frame, "search", "  API INDEX  ");
            check("D07", rows(table) == 1 && task(table, 0).title().equals("Generate Javadoc"),
                    "Notes search + status filter returned " + task(table, 0).title() + ".");
            setText(frame, "search", "no such phrase");
            check("D08", rows(table) == 0 && ui(() -> visibleLabel(frame, "No matching tasks")),
                    "No matching tasks. Rows = " + rows(table));
            setText(frame, "search", "");
            click(frame, "filter-all");
            select(table, 1);
            JDialog confirm = open(frame, "delete", "Delete task?");
            capture(images, "07-delete", confirm);
            ui(() -> { confirm.getRootPane().getDefaultButton().doClick(); return null; });
            check("D09", rows(table) == 3 && task(table, 1).title().equals("Review class diagram"),
                    "Cancel retained the selected task. Rows = " + rows(table));
            JDialog remove = open(frame, "delete", "Delete task?");
            click(remove, "confirm-delete");
            check("D10", rows(table) == 2 && task(table, 1).title().equals("Generate Javadoc"),
                    "Review class diagram removed. Rows = " + rows(table));
            capture(images, "08-deleted", frame);
            close(frame);
            TaskFlow.main(new String[0]);
            JFrame second = (JFrame) window("TaskFlow — Task Management System");
            JTable again = named(second, "tasks", JTable.class);
            check("D11", rows(again) == 2 && task(again, 0).id().equals(before.id())
                    && task(again, 0).status() == Status.COMPLETED, "Reopened with " + rows(again) + " tasks. Edited ID and status retained.");
            ui(() -> { second.setSize(second.getMinimumSize()); return null; });
            check("D12", ui(() -> named(second, "add", JButton.class).isShowing()
                    && named(second, "search", JTextField.class).getWidth() > 25
                    && again.getWidth() > 300 && again.getHeight() > 100), "Add, search and task rows visible at minimum size.");
            close(second);
            System.out.println("12/12 desktop checks passed.");
        } finally {
            ui(() -> { for (Window w : Window.getWindows()) w.dispose(); return null; });
            ui(() -> null);
            try (var paths = Files.walk(data)) {
                for (Path p : paths.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(p);
            }
        }
    }

    private static void check(String id, boolean passed, String result) {
        if (!passed) throw new AssertionError(id + " | FAIL | " + result);
        System.out.println(id + " | PASS | " + result);
    }

    private static <T> T ui(Callable<T> work) throws Exception {
        FutureTask<T> future = new FutureTask<>(work);
        SwingUtilities.invokeAndWait(future);
        return future.get();
    }

    private static Window window(String title) throws Exception {
        for (int i = 0; i < 120; i++) {
            Window found = ui(() -> {
                for (Window w : Window.getWindows()) {
                    String name = w instanceof Frame f ? f.getTitle() : w instanceof Dialog d ? d.getTitle() : "";
                    if (w.isShowing() && title.equals(name)) return w;
                }
                return null;
            });
            if (found != null) return found;
            Thread.sleep(50);
        }
        throw new AssertionError("Window did not appear: " + title);
    }

    private static Component find(Container root, String name) {
        for (Component c : root.getComponents()) {
            if (name.equals(c.getName())) return c;
            if (c instanceof Container group) {
                Component child = find(group, name);
                if (child != null) return child;
            }
        }
        return null;
    }

    private static <T extends Component> T named(Container root, String name, Class<T> type) {
        Component result = find(root, name);
        if (!type.isInstance(result)) throw new AssertionError("Missing control: " + name);
        return type.cast(result);
    }

    private static boolean visibleLabel(Container root, String text) {
        for (Component c : root.getComponents()) {
            if (c instanceof JLabel label && text.equals(label.getText()) && c.isShowing()) return true;
            if (c instanceof Container group && visibleLabel(group, text)) return true;
        }
        return false;
    }

    private static JDialog open(Window parent, String button, String title) throws Exception {
        SwingUtilities.invokeLater(() -> named(parent, button, JButton.class).doClick());
        return (JDialog) window(title);
    }

    private static void click(Window w, String name) throws Exception {
        ui(() -> { named(w, name, JButton.class).doClick(); return null; });
    }

    private static void setText(Window w, String name, String text) throws Exception {
        ui(() -> { named(w, name, JTextComponent.class).setText(text); return null; });
    }

    private static void fill(Window w, String title, String notes, Status status) throws Exception {
        setText(w, "title", title);
        setText(w, "description", notes);
        ui(() -> { named(w, "status", JComboBox.class).setSelectedItem(status); return null; });
    }

    private static void addTask(JFrame frame, String title, String notes, Status status) throws Exception {
        JDialog dialog = open(frame, "add", "Add task");
        fill(dialog, title, notes, status);
        click(dialog, "save");
    }

    private static void select(JTable table, int row) throws Exception {
        ui(() -> { table.setRowSelectionInterval(row, row); return null; });
    }

    private static int rows(JTable table) throws Exception { return ui(table::getRowCount); }

    private static Task task(JTable table, int row) throws Exception {
        return ui(() -> (Task) table.getValueAt(row, 0));
    }

    private static void close(JFrame frame) throws Exception {
        ui(() -> { frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING)); return null; });
        ui(() -> null);
    }

    private static void capture(Path destination, String name, Window window) throws Exception {
        if (destination == null) return;
        Robot robot = new Robot();
        robot.waitForIdle();
        Toolkit.getDefaultToolkit().sync();
        robot.delay(180);
        Rectangle bounds = ui(() -> {
            Container body = ((RootPaneContainer) window).getContentPane();
            return new Rectangle(body.getLocationOnScreen(), body.getSize());
        });
        ImageIO.write(robot.createScreenCapture(bounds), "png", destination.resolve(name + ".png").toFile());
    }
}
