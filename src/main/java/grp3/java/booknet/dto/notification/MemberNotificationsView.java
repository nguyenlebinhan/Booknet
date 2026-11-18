package grp3.java.booknet.dto.notification;

import grp3.java.booknet.enums.notification.NotificationType;

import java.time.LocalDateTime;
import java.util.List;

public record MemberNotificationsView(
        long allCount,
        long unreadCount,
        long loansCount,
        long reservationsCount,
        String activeFilter,
        String memberEmail,
        List<NotificationItemDto> notifications,
        NotificationPreferenceForm preferences
) {
    public record NotificationItemDto(
            Long id,
            NotificationType type,
            String title,
            String message,
            boolean read,
            boolean emailSent,
            LocalDateTime createdAt,
            String actionUrl,
            String actionLabel,
            String tone
    ) {}
}
