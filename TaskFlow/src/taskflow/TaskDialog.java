package taskflow;

import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.util.NoSuchElementException;
import javax.swing.*;

/**
 * Modal task editor and deletion confirmation. A successful save closes the dialog.
 * @author TaskFlow project
 */
@SuppressWarnings("serial")
final class TaskDialog extends JDialog {
    private final JPanel fields = Theme.panel(null, Theme.WHITE);
    private final JPanel actions = Theme.panel(new FlowLayout(FlowLayout.RIGHT, 8, 0), Theme.WHITE);
    private final JLabel error = Theme.label(" ", 13, Font.BOLD, Theme.RED);
    private final JButton cancel = Theme.button("Cancel", Theme.CANVAS, Theme.INK);
    private Task saved;

    /** Builds a modal dialog with shared action, error and keyboard behavior. */
    private TaskDialog(Window parent, String title) {
        super(parent, title, ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        JPanel body = Theme.panel(new BorderLayout(0, 15), Theme.WHITE);
        body.setBorder(BorderFactory.createEmptyBorder(18, 24, 18, 24));
        body.add(Theme.label(title, 23, Font.BOLD, Theme.INK), BorderLayout.NORTH);
        fields.setLayout(new BoxLayout(fields, BoxLayout.Y_AXIS));
        body.add(fields, BorderLayout.CENTER);
        JPanel bottom = Theme.panel(new BorderLayout(0, 10), Theme.WHITE);
        error.setName("form-error");
        bottom.add(error, BorderLayout.NORTH);
        cancel.addActionListener(e -> dispose());
        actions.add(cancel);
        bottom.add(actions, BorderLayout.SOUTH);
        body.add(bottom, BorderLayout.SOUTH);
        setContentPane(body);
        getRootPane().registerKeyboardAction(e -> dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    /**
     * Opens a new-task or edit form and persists it after validation.
     * @param parent owning window
     * @param manager open workspace
     * @param original task to edit, or null to add
     * @return saved task, or null after cancellation
     */
    static Task edit(Window parent, TaskManager manager, Task original) {
        TaskDialog dialog = new TaskDialog(parent, original == null ? "Add task" : "Edit task");
        JTextField title = new JTextField(original == null ? "" : original.title(), 34);
        title.setName("title");
        title.setFont(Theme.font(14, Font.PLAIN));
        title.setBorder(Theme.inputBorder());
        title.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            private void clearTitleError() {
                if (!title.getText().isBlank() && "Enter a task title.".equals(dialog.error.getText())) {
                    dialog.error.setText(" ");
                }
            }
            public void insertUpdate(javax.swing.event.DocumentEvent e) { clearTitleError(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { clearTitleError(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { clearTitleError(); }
        });
        JTextArea notes = new JTextArea(original == null ? "" : original.description(), 4, 34);
        notes.setName("description");
        notes.setFont(Theme.font(14, Font.PLAIN));
        notes.setLineWrap(true);
        notes.setWrapStyleWord(true);
        notes.setBorder(BorderFactory.createEmptyBorder(4, 5, 4, 5));
        JScrollPane noteScroll = new JScrollPane(notes);
        noteScroll.setBorder(BorderFactory.createLineBorder(Theme.LINE));
        JComboBox<Status> status = new JComboBox<>(Status.values());
        status.setName("status");
        status.setSelectedItem(original == null ? Status.PENDING : original.status());
        status.setBackground(Theme.WHITE);
        status.setFont(Theme.font(14, Font.PLAIN));
        dialog.field("Title", title, title);
        dialog.field("Description (optional)", noteScroll, notes);
        dialog.field("Status", status, status);
        JButton save = Theme.button(original == null ? "Add task" : "Save changes", Theme.TEAL, Theme.WHITE);
        save.setName("save");
        save.addActionListener(e -> {
            try {
                Status selected = (Status) status.getSelectedItem();
                dialog.saved = original == null
                        ? manager.add(title.getText(), notes.getText(), selected)
                        : manager.update(original.id(), title.getText(), notes.getText(), selected);
                dialog.dispose();
            } catch (IllegalArgumentException | NoSuchElementException ex) {
                dialog.error.setText(ex.getMessage());
                title.requestFocusInWindow();
            } catch (IOException ex) {
                dialog.saveError(ex);
            }
        });
        dialog.actions.add(save);
        dialog.getRootPane().setDefaultButton(save);
        dialog.showForm(parent, title);
        return dialog.saved;
    }

    /**
     * Confirms a deletion with the selected title before changing the workspace.
     * @param parent owning window
     * @param manager open workspace
     * @param task selected task
     * @return true if the deletion was saved
     */
    static boolean delete(Window parent, TaskManager manager, Task task) {
        TaskDialog dialog = new TaskDialog(parent, "Delete task?");
        JTextArea summary = new JTextArea("“" + task.title() + "” will be removed from this workspace.", 3, 32);
        summary.setFont(Theme.font(15, Font.PLAIN));
        summary.setForeground(Theme.INK);
        summary.setLineWrap(true);
        summary.setWrapStyleWord(true);
        summary.setEditable(false);
        summary.setFocusable(false);
        JScrollPane preview = new JScrollPane(summary);
        preview.setBorder(null);
        dialog.fields.add(preview);
        JButton remove = Theme.button("Delete task", Theme.RED, Theme.WHITE);
        remove.setName("confirm-delete");
        remove.addActionListener(e -> {
            try {
                manager.delete(task.id());
                dialog.saved = task;
                dialog.dispose();
            } catch (NoSuchElementException ex) {
                dialog.error.setText(ex.getMessage());
            } catch (IOException ex) {
                dialog.saveError(ex);
            }
        });
        dialog.actions.add(remove);
        dialog.getRootPane().setDefaultButton(dialog.cancel);
        dialog.showForm(parent, dialog.cancel);
        return dialog.saved != null;
    }

    /** Adds a label and its input to the form in keyboard traversal order. */
    private void field(String caption, JComponent component, JComponent input) {
        JLabel label = Theme.label(caption, 13, Font.BOLD, Theme.INK);
        label.setLabelFor(input);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        component.setAlignmentX(Component.LEFT_ALIGNMENT);
        fields.add(label);
        fields.add(Box.createVerticalStrut(5));
        fields.add(component);
        fields.add(Box.createVerticalStrut(10));
    }

    /** Displays an actionable save error and retains the failure detail in a tooltip. */
    private void saveError(IOException ex) {
        error.setText("Could not save. Check disk space and write access.");
        error.setToolTipText(ex.getMessage());
    }

    /** Places the dialog over its owner and gives focus to its first useful control. */
    private void showForm(Window parent, JComponent firstFocus) {
        pack();
        setMinimumSize(getSize());
        setLocationRelativeTo(parent);
        addWindowListener(new WindowAdapter() {
            @Override public void windowOpened(WindowEvent e) { firstFocus.requestFocusInWindow(); }
        });
        setVisible(true);
    }
}
