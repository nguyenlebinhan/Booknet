package grp3.java.booknet.dto.admin;

import java.time.LocalDateTime;

/**
 * Một dòng trong hộp "Recent administrative activity" (UC43).
 */
public record RecentActivityItem(
        String action,
        String description,
        String actorEmail,
        String actorRole,
        String targetType,
        Long targetId,
        LocalDateTime occurredAt
) {
    /** Label ngắn hiển thị bên cạnh icon (ví dụ: "Role changed", "Fine confirmed"). */
    public String actionLabel() {
        return switch (action) {
            case "ROLE_CHANGED"       -> "Role changed";
            case "ACCOUNT_SUSPENDED"  -> "Account suspended";
            case "ACCOUNT_ACTIVATED"  -> "Account activated";
            case "FINE_WAIVED"        -> "Fine waived";
            case "FINE_CONFIRMED"     -> "Fine confirmed";
            case "RETURN_CONFIRMED"   -> "Return confirmed";
            case "BOOK_PUBLISHED"     -> "Book published";
            case "BOOK_UNPUBLISHED"   -> "Book unpublished";
            case "POLICY_UPDATED"     -> "Policy updated";
            default                   -> action.replace('_', ' ').toLowerCase();
        };
    }

    /** Badge color class. */
    public String badgeClass() {
        return switch (action) {
            case "FINE_WAIVED", "ACCOUNT_SUSPENDED" -> "badge-warn";
            case "ROLE_CHANGED", "POLICY_UPDATED"   -> "badge-info";
            default                                  -> "badge-default";
        };
    }
}
