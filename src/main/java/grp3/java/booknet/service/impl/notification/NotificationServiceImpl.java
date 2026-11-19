package grp3.java.booknet.service.impl.notification;

import grp3.java.booknet.dto.notification.MemberNotificationsView;
import grp3.java.booknet.dto.notification.NotificationPreferenceForm;
import grp3.java.booknet.entity.book_service.Loan;
import grp3.java.booknet.entity.book_service.Reservation;
import grp3.java.booknet.entity.notification.Notification;
import grp3.java.booknet.entity.notification.NotificationPreference;
import grp3.java.booknet.entity.user.User;
import grp3.java.booknet.enums.book_service.LoanStatus;
import grp3.java.booknet.enums.notification.NotificationType;
import grp3.java.booknet.exception.account.AccountException;
import grp3.java.booknet.repository.book_service.LoanRepository;
import grp3.java.booknet.repository.notification.NotificationPreferenceRepository;
import grp3.java.booknet.repository.notification.NotificationRepository;
import grp3.java.booknet.repository.user.UserRepository;
import grp3.java.booknet.service.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private static final int DUE_SOON_DAYS = 3;
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATE_TIME_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy · HH:mm");

    private static final Set<LoanStatus> ACTIVE_LOAN_STATUSES = Set.of(
            LoanStatus.BORROWING,
            LoanStatus.RENEW_PENDING,
            LoanStatus.RETURN_PENDING,
            LoanStatus.OVERDUE
    );

    private static final Set<NotificationType> LOAN_TYPES = Set.of(
            NotificationType.DUE_SOON,
            NotificationType.OVERDUE,
            NotificationType.FINE,
            NotificationType.SYSTEM
    );

    private static final Set<NotificationType> RESERVATION_TYPES = Set.of(
            NotificationType.RESERVATION_READY,
            NotificationType.RESERVATION_EXPIRED
    );

    private final NotificationRepository notificationRepository;
    private final NotificationPreferenceRepository preferenceRepository;
    private final UserRepository userRepository;
    private final LoanRepository loanRepository;
    private final JavaMailSender mailSender;

    @Value("${app.account.mail-from:no-reply@booknet.local}")
    private String mailFrom;

    @Value("${app.notification.auto-email.enabled:false}")
    private boolean autoEmailEnabled;

    @Override
    @Transactional
    public MemberNotificationsView getMemberNotificationsView(Long userId, String filter) {
        User user = findUser(userId);
        NotificationPreference preference = getOrCreatePreference(userId);

        String normalizedFilter = normalizeFilter(filter);
        List<Notification> all = notificationRepository.findByUser_IdOrderByCreatedAtDescIdDesc(userId);

        long allCount = all.size();
        long unreadCount = all.stream().filter(n -> !n.isRead()).count();
        long loansCount = all.stream().filter(n -> LOAN_TYPES.contains(n.getType())).count();
        long reservationsCount = all.stream().filter(n -> RESERVATION_TYPES.contains(n.getType())).count();

        List<Notification> filtered = switch (normalizedFilter) {
            case "unread" -> all.stream().filter(n -> !n.isRead()).toList();
            case "loans" -> all.stream().filter(n -> LOAN_TYPES.contains(n.getType())).toList();
            case "reservations" -> all.stream().filter(n -> RESERVATION_TYPES.contains(n.getType())).toList();
            default -> all;
        };

        List<MemberNotificationsView.NotificationItemDto> items = filtered.stream()
                .map(this::mapToItem)
                .toList();

        NotificationPreferenceForm prefForm = NotificationPreferenceForm.builder()
                .websiteEnabled(preference.isWebsiteEnabled())
                .emailEnabled(preference.isEmailEnabled())
                .eventsNewsletterEnabled(preference.isEventsNewsletterEnabled())
                .build();

        return new MemberNotificationsView(
                allCount,
                unreadCount,
                loansCount,
                reservationsCount,
                normalizedFilter,
                user.getEmail(),
                items,
                prefForm
        );
    }

    @Override
    @Transactional(readOnly = true)
    public long countUnread(Long userId) {
        if (userId == null) {
            return 0L;
        }
        return notificationRepository.countByUser_IdAndReadFalse(userId);
    }

    @Override
    @Transactional
    public void markAsRead(Long userId, Long notificationId) {
        Notification notification = notificationRepository.findByIdAndUser_Id(notificationId, userId)
                .orElseThrow(() -> new AccountException(HttpStatus.NOT_FOUND, "Notification not found."));
        if (!notification.isRead()) {
            notification.setRead(true);
            notificationRepository.save(notification);
        }
    }

    @Override
    @Transactional
    public int markAllAsRead(Long userId) {
        return notificationRepository.markAllAsReadByUserId(userId);
    }

    @Override
    @Transactional
    public void updatePreferences(Long userId, NotificationPreferenceForm form) {
        NotificationPreference pref = getOrCreatePreference(userId);
        pref.setWebsiteEnabled(form.isWebsiteEnabled());
        pref.setEmailEnabled(form.isEmailEnabled());
        pref.setEventsNewsletterEnabled(form.isEventsNewsletterEnabled());
        preferenceRepository.save(pref);
    }

    @Override
    @Transactional
    public NotificationPreference getOrCreatePreference(Long userId) {
        return preferenceRepository.findByUser_Id(userId)
                .orElseGet(() -> {
                    User user = findUser(userId);
                    return preferenceRepository.save(NotificationPreference.builder()
                            .user(user)
                            .websiteEnabled(true)
                            .emailEnabled(true)
                            .eventsNewsletterEnabled(false)
                            .build());
                });
    }

    @Override
    @Transactional
    public void notifyReservationCreated(Reservation reservation) {
        User member = reservation.getMember();
        String bookTitle = reservation.getBook().getTitle();
        String title = "Reservation placed: " + truncate(bookTitle, 110);
        String message = "You joined the reservation queue for \"" + bookTitle + "\" ("
                + reservation.displayCode() + "). booknet will notify you when a copy is held for pickup.";
        createNotificationIfAllowed(member, NotificationType.SYSTEM, title, message, null, reservation);
    }

    @Override
    @Transactional
    public void notifyReservationReady(Reservation reservation) {
        User member = reservation.getMember();
        if (notificationRepository.existsByUser_IdAndReservation_IdAndType(
                member.getId(), reservation.getId(), NotificationType.RESERVATION_READY)) {
            return;
        }
        String bookTitle = reservation.getBook().getTitle();
        String deadline = reservation.getPickupUntil() != null
                ? reservation.getPickupUntil().format(DATE_TIME_FMT)
                : "within 2 days";
        String copyInfo = reservation.getAssignedCopy() != null
                ? " Copy " + reservation.getAssignedCopy().getBarcode() + " is held at the circulation desk."
                : "";
        String title = "Your reserved book is ready";
        String message = truncate(bookTitle + " is held for you until " + deadline + "." + copyInfo
                + " Bring your member card to collect it at the circulation desk.", 495);
        createNotificationIfAllowed(member, NotificationType.RESERVATION_READY, title, message, null, reservation);
    }

    @Override
    @Transactional
    public void notifyReservationExpired(Reservation reservation) {
        User member = reservation.getMember();
        if (notificationRepository.existsByUser_IdAndReservation_IdAndType(
                member.getId(), reservation.getId(), NotificationType.RESERVATION_EXPIRED)) {
            return;
        }
        String bookTitle = reservation.getBook().getTitle();
        String title = "Reservation hold expired";
        String message = truncate("Your pickup window for \"" + bookTitle + "\" ("
                + reservation.displayCode() + ") has expired, and the copy has been released to the queue.", 495);
        createNotificationIfAllowed(member, NotificationType.RESERVATION_EXPIRED, title, message, null, reservation);
    }

    @Override
    @Transactional
    public void notifyReservationCancelled(Reservation reservation, boolean byStaff) {
        User member = reservation.getMember();
        String bookTitle = reservation.getBook().getTitle();
        String title = "Reservation cancelled";
        String message = truncate((byStaff ? "A librarian cancelled your reservation " : "You cancelled your reservation ")
                + reservation.displayCode() + " for \"" + bookTitle + "\".", 495);
        createNotificationIfAllowed(member, NotificationType.SYSTEM, title, message, null, reservation);
    }

    @Override
    @Transactional
    public void notifyReservationFulfilled(Reservation reservation) {
        User member = reservation.getMember();
        String bookTitle = reservation.getBook().getTitle();
        String title = "Reserved book checked out";
        String message = truncate("Your reservation " + reservation.displayCode() + " for \""
                + bookTitle + "\" has been picked up and converted into an active loan.", 495);
        createNotificationIfAllowed(member, NotificationType.SYSTEM, title, message, null, reservation);
    }

    @Override
    @Transactional
    public int generateAutomatedLoanReminders() {
        LocalDate today = LocalDate.now();
        LocalDate dueSoonCutoff = today.plusDays(DUE_SOON_DAYS);
        LocalDateTime startOfToday = today.atStartOfDay();
        int created = 0;

        List<Loan> dueSoonLoans = loanRepository.findByStatusInAndDueDateBetweenOrderByDueDateAsc(
                ACTIVE_LOAN_STATUSES, today, dueSoonCutoff);
        for (Loan loan : dueSoonLoans) {
            User member = loan.getMember();
            if (notificationRepository.existsByUser_IdAndLoan_IdAndType(
                    member.getId(), loan.getId(), NotificationType.DUE_SOON)) {
                continue;
            }
            String bookTitle = loan.getCopy().getBook().getTitle();
            long daysLeft = ChronoUnit.DAYS.between(today, loan.getDueDate());
            String when = daysLeft == 0 ? "today" : "in " + daysLeft + " day(s)";
            String title = truncate(bookTitle + " is due back soon", 145);
            String message = truncate(bookTitle + " is due " + when + " ("
                    + loan.getDueDate().format(DATE_FMT)
                    + "). You may renew for 7 more days if there are no reservations.", 495);
            if (createNotificationIfAllowed(member, NotificationType.DUE_SOON, title, message, loan, null)) {
                created++;
            }
        }

        List<Loan> overdueLoans = loanRepository.findByStatusInAndDueDateBeforeOrderByDueDateAsc(
                ACTIVE_LOAN_STATUSES, today);
        for (Loan loan : overdueLoans) {
            User member = loan.getMember();
            if (notificationRepository.existsByUser_IdAndLoan_IdAndTypeAndCreatedAtAfter(
                    member.getId(), loan.getId(), NotificationType.OVERDUE, startOfToday)) {
                continue;
            }
            String bookTitle = loan.getCopy().getBook().getTitle();
            long overdueDays = ChronoUnit.DAYS.between(loan.getDueDate(), today);
            String title = truncate("Overdue reminder: " + bookTitle, 145);
            String message = truncate(bookTitle + " is " + overdueDays + " day(s) overdue, due on "
                    + loan.getDueDate().format(DATE_FMT)
                    + ". Please return it to the circulation desk. Ignore this reminder if already returned.", 495);
            if (createNotificationIfAllowed(member, NotificationType.OVERDUE, title, message, loan, null)) {
                created++;
            }
        }

        return created;
    }

    private boolean createNotificationIfAllowed(User user,
                                                NotificationType type,
                                                String title,
                                                String message,
                                                Loan loan,
                                                Reservation reservation) {
        NotificationPreference pref = getOrCreatePreference(user.getId());
        boolean sentEmail = false;
        if (autoEmailEnabled && pref.isEmailEnabled() && user.getEmail() != null && !user.getEmail().isBlank()) {
            sentEmail = trySendAutomaticEmail(user.getEmail(), title, message);
        }

        if (!pref.isWebsiteEnabled()) {
            return false;
        }

        Notification notification = Notification.builder()
                .user(user)
                .type(type)
                .title(truncate(title, 150))
                .message(truncate(message, 500))
                .loan(loan)
                .reservation(reservation)
                .read(false)
                .emailSent(sentEmail)
                .build();
        notificationRepository.save(notification);
        return true;
    }

    private boolean trySendAutomaticEmail(String to, String subject, String body) {
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(mailFrom);
            msg.setTo(to);
            msg.setSubject("booknet Library - " + subject);
            msg.setText(body + "\n\n--\nbooknet Community Library");
            mailSender.send(msg);
            return true;
        } catch (MailException ex) {
            log.warn("Automatic notification email failed ({})", ex.getClass().getSimpleName());
            return false;
        }
    }

    private MemberNotificationsView.NotificationItemDto mapToItem(Notification n) {
        String actionUrl = "/member/my-books";
        String actionLabel = "View current loans →";
        String tone = "system";

        switch (n.getType()) {
            case RESERVATION_READY -> {
                actionUrl = "/member/reservations";
                actionLabel = "View reservation →";
                tone = "ready";
            }
            case RESERVATION_EXPIRED -> {
                actionUrl = "/member/reservations?tab=canceled";
                actionLabel = "View reservations →";
                tone = "expired";
            }
            case DUE_SOON -> {
                actionUrl = "/member/my-books";
                actionLabel = "View & renew →";
                tone = "due-soon";
            }
            case OVERDUE -> {
                actionUrl = n.getLoan() != null
                        ? "/member/loans/" + n.getLoan().getId() + "/return-detail"
                        : "/member/my-books";
                actionLabel = "View return details →";
                tone = "overdue";
            }
            case FINE -> {
                actionUrl = "/member/my-books";
                actionLabel = "View loan details →";
                tone = "overdue";
            }
            case SYSTEM -> {
                if (n.getReservation() != null) {
                    actionUrl = "/member/reservations";
                    actionLabel = "View reservations →";
                } else if (n.getLoan() != null) {
                    actionUrl = "/member/loans/" + n.getLoan().getId() + "/return-detail";
                    actionLabel = "View loan details →";
                }
                tone = "system";
            }
        }

        return new MemberNotificationsView.NotificationItemDto(
                n.getId(),
                n.getType(),
                n.getTitle(),
                n.getMessage(),
                n.isRead(),
                n.isEmailSent(),
                n.getCreatedAt(),
                actionUrl,
                actionLabel,
                tone
        );
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new AccountException(HttpStatus.UNAUTHORIZED, "User account not found."));
    }

    private static String normalizeFilter(String filter) {
        if (filter == null) {
            return "all";
        }
        return switch (filter.trim().toLowerCase()) {
            case "unread", "loans", "reservations" -> filter.trim().toLowerCase();
            default -> "all";
        };
    }

    private static String truncate(String value, int maxLen) {
        if (value == null) {
            return "";
        }
        return value.length() <= maxLen ? value : value.substring(0, maxLen);
    }
}
