package taskflow;

import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.nio.file.Path;
import javax.swing.*;

/**
 * Starts the TaskFlow desktop application and keeps its workspace open until the window closes.
 * @author TaskFlow project
 */
public final class TaskFlow {
    /** Prevents construction of the application entry point. */
    private TaskFlow() { }

    /** Starts the Swing event-dispatch thread.
     * @param args command-line arguments, unused
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(TaskFlow::open);
    }

    /** Opens the workspace on the event thread and releases its lock when the window closes. */
    private static void open() {
        Path directory = Path.of(System.getProperty("taskflow.dir",
                Path.of(System.getProperty("user.home"), ".taskflow").toString()));
        try {
            TaskStore store = new TaskStore(directory);
            try {
                TaskManager manager = new TaskManager(store);
                JFrame frame = new JFrame("TaskFlow — Task Management System");
                frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
                frame.setContentPane(new TaskBoard(manager));
                frame.pack();
                frame.setMinimumSize(new Dimension(790, 500));
                frame.setSize(920, 610);
                frame.setLocationRelativeTo(null);
                frame.addWindowListener(new WindowAdapter() {
                    @Override public void windowClosed(WindowEvent e) {
                        try { store.close(); }
                        catch (IOException ex) { System.err.println(ex.getMessage()); }
                    }
                });
                frame.setVisible(true);
            } catch (IOException | RuntimeException e) {
                store.close();
                throw e;
            }
        } catch (IOException | SecurityException e) {
            JOptionPane.showMessageDialog(null, e.getMessage(), "Cannot open workspace", JOptionPane.ERROR_MESSAGE);
        }
    }
}
