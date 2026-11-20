package taskflow;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * Main workspace with status counts, a text search and the list of matching tasks.
 * @author TaskFlow project
 */
@SuppressWarnings("serial")
final class TaskBoard extends JPanel {
    private final TaskManager manager;
    private final TaskTable table = new TaskTable();
    private final JTextField search = new JTextField(15);
    private final JButton[] filters = new JButton[Status.values().length + 1];
    private final CardLayout cards = new CardLayout();
    private final JPanel content = new JPanel(cards);
    private final JLabel emptyTitle = Theme.label("", 21, Font.BOLD, Theme.INK);
    private final JLabel emptyHint = Theme.label("", 14, Font.PLAIN, Theme.MUTED);
    private final JLabel feedback = Theme.label("", 13, Font.PLAIN, Theme.MUTED);
    private final JButton edit = Theme.button("Edit task", Theme.PALE, Theme.TEAL);
    private final JButton delete = Theme.button("Delete", new Color(0xF6E9E5), Theme.RED);
    private Status filter;

    /** Builds the workspace bound to a loaded task manager.
     * @param manager active workspace
     */
    TaskBoard(TaskManager manager) {
        super(new BorderLayout());
        this.manager = manager;
        add(header(), BorderLayout.NORTH);
        JPanel body = Theme.panel(new BorderLayout(0, 17), Theme.CANVAS);
        body.setBorder(BorderFactory.createEmptyBorder(24, 30, 18, 30));
        JPanel top = Theme.panel(new BorderLayout(0, 17), Theme.CANVAS);
        JPanel titleRow = Theme.panel(new BorderLayout(), Theme.CANVAS);
        titleRow.add(Theme.label("My tasks", 32, Font.BOLD, Theme.INK), BorderLayout.WEST);
        JButton add = Theme.button("+  Add task", Theme.TEAL, Theme.WHITE);
        add.setName("add");
        add.addActionListener(e -> editTask(null));
        titleRow.add(add, BorderLayout.EAST);
        top.add(titleRow, BorderLayout.NORTH);
        top.add(toolbar(), BorderLayout.SOUTH);
        body.add(top, BorderLayout.NORTH);
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(Theme.WHITE);
        content.setBorder(BorderFactory.createLineBorder(Theme.LINE));
        content.add(scroll, "list");
        content.add(emptyState(), "empty");
        body.add(content, BorderLayout.CENTER);
        JPanel footer = Theme.panel(new BorderLayout(), Theme.CANVAS);
        footer.add(feedback, BorderLayout.WEST);
        JPanel actions = Theme.panel(new FlowLayout(FlowLayout.RIGHT, 6, 0), Theme.CANVAS);
        edit.setName("edit");
        delete.setName("delete");
        edit.addActionListener(e -> editSelection());
        delete.addActionListener(e -> {
            Task selected = table.selectedTask();
            if (selected != null && TaskDialog.delete(SwingUtilities.getWindowAncestor(this), manager, selected)) {
                refresh();
                feedback.setText("Task deleted.  " + feedback.getText());
            }
        });
        actions.add(edit);
        actions.add(delete);
        footer.add(actions, BorderLayout.EAST);
        table.getSelectionModel().addListSelectionListener(e -> updateActions());
        table.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && table.rowAtPoint(e.getPoint()) >= 0) editSelection();
            }
        });
        updateActions();
        body.add(footer, BorderLayout.SOUTH);
        add(body, BorderLayout.CENTER);
        refresh();
    }

    /** Enables row actions only when the table has a selection. */
    private void updateActions() {
        boolean selected = table.selectedTask() != null;
        edit.setEnabled(selected);
        delete.setEnabled(selected);
    }

    /** Opens the editor for the currently selected task. */
    private void editSelection() {
        Task selected = table.selectedTask();
        if (selected != null) editTask(selected);
    }

    /** Saves a new or revised task, refreshes the table and reveals the saved row if visible. */
    private void editTask(Task original) {
        Task saved = TaskDialog.edit(SwingUtilities.getWindowAncestor(this), manager, original);
        if (saved == null) return;
        if (original == null) {
            filter = null;
            search.setText("");
        }
        refresh();
        table.reveal(saved);
        feedback.setText((original == null ? "Task added.  " : "Changes saved.  ") + feedback.getText());
    }

    /** Builds the brand bar and local storage indicator. */
    private JPanel header() {
        JPanel bar = Theme.panel(new BorderLayout(), Theme.INK);
        bar.setBorder(BorderFactory.createEmptyBorder(14, 30, 14, 30));
        JLabel brand = Theme.label("TaskFlow", 21, Font.BOLD, Theme.WHITE);
        brand.setIcon(Theme.mark(33));
        brand.setIconTextGap(11);
        bar.add(brand, BorderLayout.WEST);
        bar.add(Theme.label("Local workspace", 13, Font.PLAIN, new Color(0xBDD5CC)), BorderLayout.EAST);
        return bar;
    }

    /** Builds status filters and a live title-and-description search field. */
    private JPanel toolbar() {
        JPanel line = Theme.panel(new BorderLayout(10, 0), Theme.CANVAS);
        JPanel tabs = Theme.panel(new FlowLayout(FlowLayout.LEFT, 4, 0), Theme.CANVAS);
        for (int i = 0; i < filters.length; i++) {
            Status choice = i == 0 ? null : Status.values()[i - 1];
            JButton button = Theme.button("", Theme.CANVAS, Theme.MUTED);
            button.setName(choice == null ? "filter-all" : "filter-" + choice.name());
            button.addActionListener(e -> { filter = choice; refresh(); });
            filters[i] = button;
            tabs.add(button);
        }
        line.add(tabs, BorderLayout.WEST);
        JPanel searchBox = Theme.panel(new BorderLayout(6, 0), Theme.WHITE);
        searchBox.setBorder(Theme.inputBorder());
        JLabel label = Theme.label("Search", 12, Font.BOLD, Theme.MUTED);
        label.setLabelFor(search);
        search.setName("search");
        search.setBorder(null);
        search.setFont(Theme.font(13, Font.PLAIN));
        search.setToolTipText("Find text in titles or descriptions");
        searchBox.add(label, BorderLayout.WEST);
        searchBox.add(search, BorderLayout.CENTER);
        search.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { refresh(); }
            public void removeUpdate(DocumentEvent e) { refresh(); }
            public void changedUpdate(DocumentEvent e) { refresh(); }
        });
        line.add(searchBox, BorderLayout.CENTER);
        return line;
    }

    /** Builds the message shown when the workspace or current view contains no tasks. */
    private JPanel emptyState() {
        JPanel center = Theme.panel(new GridBagLayout(), Theme.WHITE);
        JPanel stack = Theme.panel(new GridLayout(2, 1, 0, 6), Theme.WHITE);
        emptyTitle.setHorizontalAlignment(SwingConstants.CENTER);
        emptyHint.setHorizontalAlignment(SwingConstants.CENTER);
        stack.add(emptyTitle);
        stack.add(emptyHint);
        center.add(stack);
        return center;
    }

    /** Recomputes counts and the intersection of the status and text filters. */
    private void refresh() {
        List<Task> all = manager.all();
        List<Task> visible = new ArrayList<>();
        int[] counts = new int[filters.length];
        counts[0] = all.size();
        String query = search.getText().strip().toLowerCase(Locale.ROOT);
        for (Task task : all) {
            counts[task.status().ordinal() + 1]++;
            boolean statusMatches = filter == null || filter == task.status();
            boolean textMatches = task.title().toLowerCase(Locale.ROOT).contains(query)
                    || task.description().toLowerCase(Locale.ROOT).contains(query);
            if (statusMatches && textMatches) visible.add(task);
        }
        for (int i = 0; i < filters.length; i++) {
            Status choice = i == 0 ? null : Status.values()[i - 1];
            filters[i].setText((choice == null ? "All tasks" : choice.toString()) + "  " + counts[i]);
            filters[i].setBackground(filter == choice ? Theme.INK : Theme.CANVAS);
            filters[i].setForeground(filter == choice ? Theme.WHITE : Theme.MUTED);
        }
        table.showTasks(visible);
        emptyTitle.setText(all.isEmpty() ? "No tasks yet" : "No matching tasks");
        emptyHint.setText(all.isEmpty() ? "Add your first task to start this workspace."
                : "Change the status filter or search text.");
        cards.show(content, visible.isEmpty() ? "empty" : "list");
        feedback.setText(visible.size() + " of " + all.size() + (all.size() == 1 ? " task shown" : " tasks shown"));
    }
}
