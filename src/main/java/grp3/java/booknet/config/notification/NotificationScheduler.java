package grp3.java.booknet.config.notification;

import grp3.java.booknet.service.notification.NotificationService;
import grp3.java.booknet.service.reservation.ReservationQueueService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Slf4j
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "app.scheduling.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class NotificationScheduler {

    private final ReservationQueueService reservationQueueService;
    private final NotificationService notificationService;

    @Scheduled(cron = "${app.notification.expiration-cron:0 */30 * * * *}")
    public void processExpiredReservations() {
        int expired = reservationQueueService.expireOverdueReservations();
        if (expired > 0) {
            log.info("Expired {} overdue reservation pickup hold(s)", expired);
        }
    }

    @Scheduled(cron = "${app.notification.reminder-cron:0 0 8 * * *}")
    public void generateDailyLoanReminders() {
        int created = notificationService.generateAutomatedLoanReminders();
        if (created > 0) {
            log.info("Generated {} automated loan reminder notification(s)", created);
        }
    }
}
