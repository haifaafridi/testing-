package taskflow;

import java.awt.*;
import java.util.Arrays;
import javax.swing.*;
import javax.swing.border.Border;

/**
 * Shared colors, type and controls for the workspace and its dialogs.
 * @author TaskFlow project
 */
final class Theme {
    /** Dark ink for headers and primary text. */
    static final Color INK = new Color(0x172E34);
    /** Secondary text color. */
    static final Color MUTED = new Color(0x50666B);
    /** Primary action color. */
    static final Color TEAL = new Color(0x146D60);
    /** Selected row and secondary action fill. */
    static final Color PALE = new Color(0xDDF2EA);
    /** Workspace background. */
    static final Color CANVAS = new Color(0xF5F7F5);
    /** Borders and table dividers. */
    static final Color LINE = new Color(0xD4DEDA);
    /** Validation and deletion accent. */
    static final Color RED = new Color(0xA43A32);
    /** Form and table surface. */
    static final Color WHITE = Color.WHITE;
    private static final String FAMILY = chooseFont();

    /** Groups static styling methods and prevents construction. */
    private Theme() { }

    /** Picks the first installed family in the desktop font preference order. */
    private static String chooseFont() {
        var installed = Arrays.asList(GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getAvailableFontFamilyNames());
        for (String family : new String[]{"Inter", "Segoe UI", "Open Sans"}) {
            if (installed.contains(family)) return family;
        }
        return Font.SANS_SERIF;
    }

    /** Creates a consistent application font.
     * @param size point size
     * @param style Font.PLAIN or Font.BOLD
     * @return requested font
     */
    static Font font(int size, int style) { return new Font(FAMILY, style, size); }

    /** Makes a text label that always treats user-entered text literally.
     * @param text visible label
     * @param size point size
     * @param style font style
     * @param color foreground
     * @return configured label
     */
    static JLabel label(String text, int size, int style, Color color) {
        JLabel label = new JLabel();
        label.putClientProperty("html.disable", true);
        label.setText(text);
        label.setFont(font(size, style));
        label.setForeground(color);
        return label;
    }

    /** Creates a panel with an explicit background.
     * @param layout layout manager
     * @param color background
     * @return new panel
     */
    static JPanel panel(LayoutManager layout, Color color) {
        JPanel panel = new JPanel(layout);
        panel.setBackground(color);
        return panel;
    }

    /** Makes an inset border for text fields and description editors.
     * @return a thin outlined border with internal padding
     */
    static Border inputBorder() {
        return BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(LINE),
                BorderFactory.createEmptyBorder(7, 10, 7, 10));
    }

    /** Makes a rounded button with visible disabled, hover and keyboard focus states.
     * @param text accessible button label
     * @param bg fill color
     * @param fg text color
     * @return styled button
     */
    static JButton button(String text, Color bg, Color fg) {
        JButton result = new JButton(text) {
            @Override protected void paintComponent(Graphics graphics) {
                Graphics2D g = (Graphics2D) graphics.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color color = isEnabled() ? getBackground() : new Color(0xE7ECE9);
                if (isEnabled() && getModel().isRollover()) color = color.darker();
                g.setColor(color);
                g.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                if (isFocusOwner()) {
                    g.setColor(getForeground());
                    g.drawRoundRect(3, 3, getWidth() - 7, getHeight() - 7, 8, 8);
                }
                g.dispose();
                super.paintComponent(graphics);
            }
        };
        result.setFont(font(13, Font.BOLD));
        result.setBorder(BorderFactory.createEmptyBorder(9, 14, 9, 14));
        result.setBackground(bg);
        result.setForeground(fg);
        result.setFocusPainted(false);
        result.setContentAreaFilled(false);
        result.setOpaque(false);
        result.setRolloverEnabled(true);
        result.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return result;
    }

    /** Draws the workspace check mark in its own rounded square.
     * @param size icon side length
     * @return vector icon
     */
    static Icon mark(int size) {
        return new Icon() {
            public int getIconWidth() { return size; }
            public int getIconHeight() { return size; }
            public void paintIcon(Component c, Graphics graphics, int x, int y) {
                Graphics2D g = (Graphics2D) graphics.create();
                g.translate(x, y);
                g.scale(size / 36.0, size / 36.0);
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(new Color(0xA4E3C2));
                g.fillRoundRect(0, 0, 36, 36, 10, 10);
                g.setColor(INK);
                g.setStroke(new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.drawPolyline(new int[]{9, 15, 27}, new int[]{18, 24, 11}, 3);
                g.dispose();
            }
        };
    }
}
