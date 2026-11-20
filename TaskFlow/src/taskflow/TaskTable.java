package taskflow;

import java.awt.*;
import java.util.List;
import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;

/**
 * A read-only table that displays titles, notes previews and labelled status badges.
 * @author TaskFlow project
 */
@SuppressWarnings("serial")
final class TaskTable extends JTable {
    private final Rows rows = new Rows();

    /** Builds the table and installs renderers for its two columns. */
    TaskTable() {
        setModel(rows);
        setName("tasks");
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        setRowHeight(67);
        setShowVerticalLines(false);
        setGridColor(Theme.LINE);
        setIntercellSpacing(new Dimension(0, 1));
        setBackground(Theme.WHITE);
        setSelectionBackground(Theme.PALE);
        getTableHeader().setReorderingAllowed(false);
        getTableHeader().setPreferredSize(new Dimension(1, 36));
        getTableHeader().setDefaultRenderer((table, value, selected, focus, row, col) -> {
            JLabel heading = Theme.label(value.toString(), 12, Font.BOLD, Theme.MUTED);
            heading.setOpaque(true);
            heading.setBackground(new Color(0xEAF0EC));
            heading.setBorder(BorderFactory.createEmptyBorder(0, 18, 0, 0));
            return heading;
        });
        getColumnModel().getColumn(0).setCellRenderer(detailsRenderer());
        getColumnModel().getColumn(1).setMinWidth(136);
        getColumnModel().getColumn(1).setMaxWidth(175);
        getColumnModel().getColumn(1).setPreferredWidth(155);
        getColumnModel().getColumn(1).setCellRenderer(statusRenderer());
    }

    /** Replaces the rows after a search or mutation.
     * @param visible filtered tasks in display order
     */
    void showTasks(List<Task> visible) {
        rows.tasks = List.copyOf(visible);
        rows.fireTableDataChanged();
    }

    /** Returns the selected task.
     * @return task, or null if no row is selected
     */
    Task selectedTask() {
        int index = getSelectedRow();
        return index < 0 ? null : rows.tasks.get(convertRowIndexToModel(index));
    }

    /** Selects a saved task when it remains visible.
     * @param task task to reveal
     */
    void reveal(Task task) {
        int row = rows.tasks.indexOf(task);
        if (row >= 0) {
            setRowSelectionInterval(row, row);
            scrollRectToVisible(getCellRect(row, 0, true));
        }
    }

    /** Renders a title and the first line of its description. */
    private TableCellRenderer detailsRenderer() {
        return (table, value, selected, focus, row, col) -> {
            Task task = (Task) value;
            JPanel cell = Theme.panel(new GridLayout(2, 1), selected ? Theme.PALE : Theme.WHITE);
            cell.setBorder(BorderFactory.createEmptyBorder(8, 18, 8, 10));
            cell.add(Theme.label(task.title(), 15, Font.BOLD, Theme.INK));
            String note = task.description().isEmpty() ? "No description" : task.description().lines().findFirst().orElse("");
            cell.add(Theme.label(note, 13, Font.PLAIN, Theme.MUTED));
            return cell;
        };
    }

    /** Displays each stage with both a text label and a distinct fill color. */
    private TableCellRenderer statusRenderer() {
        return (table, value, selected, focus, row, col) -> {
            Status status = (Status) value;
            JPanel cell = Theme.panel(new GridBagLayout(), selected ? Theme.PALE : Theme.WHITE);
            GridBagConstraints layout = new GridBagConstraints();
            layout.weightx = 1;
            layout.anchor = GridBagConstraints.WEST;
            layout.insets = new Insets(0, 14, 0, 0);
            JLabel badge = Theme.label(status.toString(), 12, Font.BOLD, switch (status) {
                case PENDING -> new Color(0x825319);
                case IN_PROGRESS -> new Color(0x285D90);
                case COMPLETED -> new Color(0x236548);
            });
            badge.setOpaque(true);
            badge.setBackground(switch (status) {
                case PENDING -> new Color(0xFCEDCF);
                case IN_PROGRESS -> new Color(0xE2EDFA);
                case COMPLETED -> new Color(0xD9EFE0);
            });
            badge.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
            cell.add(badge, layout);
            return cell;
        };
    }

    /** Adapts the displayed snapshot to the table's two non-editable columns. */
    private static final class Rows extends AbstractTableModel {
        private List<Task> tasks = List.of();
        public int getRowCount() { return tasks.size(); }
        public int getColumnCount() { return 2; }
        public String getColumnName(int column) { return column == 0 ? "Task" : "Status"; }
        public Object getValueAt(int row, int column) {
            Task task = tasks.get(row);
            return column == 0 ? task : task.status();
        }
    }
}
