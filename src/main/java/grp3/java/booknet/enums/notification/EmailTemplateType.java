package grp3.java.booknet.enums.notification;

public enum EmailTemplateType {
    DUE_SOON("Due soon", "Remind members 3 days before the due date."),
    OVERDUE("Overdue books", "Remind members to return books to the desk."),
    PICKUP_READY("Reservation ready", "Notify members of the pickup deadline.");

    private final String label;
    private final String summary;

    EmailTemplateType(String label, String summary) {
        this.label = label;
        this.summary = summary;
    }

    public String getLabel() {
        return label;
    }

    public String getSummary() {
        return summary;
    }
}
