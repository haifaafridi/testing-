package taskflow;

/**
 * The three stages a task can occupy.
 * @author TaskFlow project
 */
public enum Status {
    /** Work has not started. */
    PENDING("Pending"),
    /** Work is under way. */
    IN_PROGRESS("In Progress"),
    /** Work has finished. */
    COMPLETED("Completed");

    private final String label;

    /** Associates an on-screen label with a stored status. */
    Status(String label) {
        this.label = label;
    }

    /** Returns the label used in the desktop interface.
     * @return human-readable status
     */
    @Override public String toString() {
        return label;
    }
}
