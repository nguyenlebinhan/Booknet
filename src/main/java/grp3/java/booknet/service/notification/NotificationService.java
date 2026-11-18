package grp3.java.booknet.service.notification;

import grp3.java.booknet.dto.notification.MemberNotificationsView;
import grp3.java.booknet.dto.notification.NotificationPreferenceForm;
import grp3.java.booknet.entity.book_service.Reservation;
import grp3.java.booknet.entity.notification.NotificationPreference;

public interface NotificationService {

    MemberNotificationsView getMemberNotificationsView(Long userId, String filter);

    long countUnread(Long userId);

    void markAsRead(Long userId, Long notificationId);

    int markAllAsRead(Long userId);

    void updatePreferences(Long userId, NotificationPreferenceForm form);

    NotificationPreference getOrCreatePreference(Long userId);

    void notifyReservationCreated(Reservation reservation);

    void notifyReservationReady(Reservation reservation);

    void notifyReservationExpired(Reservation reservation);

    void notifyReservationCancelled(Reservation reservation, boolean byStaff);

    void notifyReservationFulfilled(Reservation reservation);

    int generateAutomatedLoanReminders();
}
