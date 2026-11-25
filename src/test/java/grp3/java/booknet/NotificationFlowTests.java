package grp3.java.booknet;

import grp3.java.booknet.entity.book.Book;
import grp3.java.booknet.entity.book.BookCopy;
import grp3.java.booknet.entity.book_service.Loan;
import grp3.java.booknet.entity.book_service.Reservation;
import grp3.java.booknet.entity.notification.Notification;
import grp3.java.booknet.entity.notification.NotificationPreference;
import grp3.java.booknet.entity.user.User;
import grp3.java.booknet.enums.book.CopyStatus;
import grp3.java.booknet.enums.book.PublicationStatus;
import grp3.java.booknet.enums.book_service.LoanStatus;
import grp3.java.booknet.enums.book_service.ReservationStatus;
import grp3.java.booknet.enums.notification.NotificationType;
import grp3.java.booknet.enums.user.AuthProvider;
import grp3.java.booknet.enums.user.UserRole;
import grp3.java.booknet.enums.user.UserStatus;
import grp3.java.booknet.repository.book.BookCopyRepository;
import grp3.java.booknet.repository.book.BookRepository;
import grp3.java.booknet.repository.book_service.LoanRepository;
import grp3.java.booknet.repository.book_service.ReservationRepository;
import grp3.java.booknet.repository.notification.NotificationPreferenceRepository;
import grp3.java.booknet.repository.notification.NotificationRepository;
import grp3.java.booknet.repository.user.UserRepository;
import grp3.java.booknet.security.dto.CustomUserDetails;
import grp3.java.booknet.service.notification.NotificationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class NotificationFlowTests {

    @MockitoBean
    private JavaMailSender mailSender;

    @Autowired private MockMvc mvc;
    @Autowired private NotificationService notificationService;
    @Autowired private NotificationRepository notificationRepository;
    @Autowired private NotificationPreferenceRepository preferenceRepository;
    @Autowired private LoanRepository loanRepository;
    @Autowired private ReservationRepository reservationRepository;
    @Autowired private BookCopyRepository bookCopyRepository;
    @Autowired private BookRepository bookRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private User member;
    private User otherMember;
    private User librarian;
    private Book book;
    private BookCopy copyDueSoon;
    private BookCopy copyOverdue;

    @BeforeEach
    void setUp() {
        cleanAll();
        TestMail.prepare(mailSender);

        member = userRepository.saveAndFlush(User.builder()
                .fullName("Minh Anh")
                .email("minhanh.notif@booknet.test")
                .password(passwordEncoder.encode(TestPasswords.valid()))
                .memberCode("BN-NTF-01")
                .role(UserRole.ROLE_USER)
                .provider(AuthProvider.LOCAL)
                .enabled(true)
                .emailVerified(true)
                .status(UserStatus.ACTIVE)
                .build());

        otherMember = userRepository.saveAndFlush(User.builder()
                .fullName("Other Reader")
                .email("other.notif@booknet.test")
                .password(passwordEncoder.encode(TestPasswords.valid()))
                .memberCode("BN-NTF-02")
                .role(UserRole.ROLE_USER)
                .provider(AuthProvider.LOCAL)
                .enabled(true)
                .emailVerified(true)
                .status(UserStatus.ACTIVE)
                .build());

        librarian = userRepository.saveAndFlush(User.builder()
                .fullName("Librarian Lan")
                .email("lan.notif@booknet.test")
                .password(passwordEncoder.encode(TestPasswords.valid()))
                .role(UserRole.ROLE_LIBRARIAN)
                .provider(AuthProvider.LOCAL)
                .enabled(true)
                .emailVerified(true)
                .status(UserStatus.ACTIVE)
                .build());

        book = bookRepository.saveAndFlush(Book.builder()
                .bookCode("BK-NTF-01")
                .isbn("978-0201633610")
                .title("Design Patterns")
                .publicationStatus(PublicationStatus.PUBLISHED)
                .deleted(false)
                .build());

        copyDueSoon = bookCopyRepository.saveAndFlush(BookCopy.builder()
                .book(book).barcode("NTF-C01").status(CopyStatus.BORROWED).build());
        copyOverdue = bookCopyRepository.saveAndFlush(BookCopy.builder()
                .book(book).barcode("NTF-C02").status(CopyStatus.BORROWED).build());
    }

    @AfterEach
    void tearDown() {
        cleanAll();
    }

    private void cleanAll() {
        notificationRepository.deleteAll();
        preferenceRepository.deleteAll();
        loanRepository.deleteAll();
        reservationRepository.deleteAll();
        bookCopyRepository.deleteAll();
        bookRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void memberNotificationInboxFiltersMarkReadAndNavbarUnreadBadgeWorkEndToEnd() throws Exception {
        Notification n1 = notificationRepository.saveAndFlush(Notification.builder()
                .user(member)
                .type(NotificationType.RESERVATION_READY)
                .title("Book held for pickup")
                .message("Clean Code is waiting at the desk.")
                .read(false)
                .build());
        notificationRepository.saveAndFlush(Notification.builder()
                .user(member)
                .type(NotificationType.DUE_SOON)
                .title("Due in 2 days")
                .message("Design Patterns is due soon.")
                .read(false)
                .build());

        Notification foreign = notificationRepository.saveAndFlush(Notification.builder()
                .user(otherMember)
                .type(NotificationType.SYSTEM)
                .title("Private")
                .message("Other member notice")
                .read(false)
                .build());

        mvc.perform(get("/member/notifications").with(user(new CustomUserDetails(member))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("A reminder from booknet.")))
                .andExpect(content().string(containsString("Book held for pickup")))
                .andExpect(content().string(containsString("Due in 2 days")))
                .andExpect(content().string(containsString("bn-bell-badge\">2<")))
                .andExpect(content().string(not(containsString(" th:"))));

        mvc.perform(get("/member/notifications")
                        .with(user(new CustomUserDetails(member)))
                        .param("filter", "reservations"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Book held for pickup")))
                .andExpect(content().string(not(containsString("Due in 2 days"))));

        // Cannot mark another user's notification read
        mvc.perform(post("/member/notifications/" + foreign.getId() + "/read")
                        .with(user(new CustomUserDetails(member)))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection());
        assertThat(notificationRepository.findById(foreign.getId()).orElseThrow().isRead()).isFalse();

        // Mark one read
        mvc.perform(post("/member/notifications/" + n1.getId() + "/read")
                        .with(user(new CustomUserDetails(member)))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection());
        assertThat(notificationRepository.findById(n1.getId()).orElseThrow().isRead()).isTrue();
        assertThat(notificationService.countUnread(member.getId())).isEqualTo(1);

        // Mark all read
        mvc.perform(post("/member/notifications/read-all")
                        .with(user(new CustomUserDetails(member)))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection());
        assertThat(notificationService.countUnread(member.getId())).isZero();
    }

    @Test
    void notificationPreferencesSaveAndControlWebsiteDelivery() throws Exception {
        mvc.perform(post("/member/notifications/preferences")
                        .with(user(new CustomUserDetails(member)))
                        .with(csrf())
                        .param("_websiteEnabled", "on")
                        .param("_emailEnabled", "on")
                        .param("eventsNewsletterEnabled", "true")
                        .param("_eventsNewsletterEnabled", "on"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/member/notifications?filter=all"));

        NotificationPreference savedPref = preferenceRepository.findByUser_Id(member.getId()).orElseThrow();
        assertThat(savedPref.isWebsiteEnabled()).isFalse();
        assertThat(savedPref.isEmailEnabled()).isFalse();
        assertThat(savedPref.isEventsNewsletterEnabled()).isTrue();

        Reservation res = reservationRepository.saveAndFlush(Reservation.builder()
                .book(book)
                .member(member)
                .status(ReservationStatus.READY)
                .assignedCopy(copyDueSoon)
                .pickupUntil(LocalDateTime.now().plusDays(2))
                .build());
        notificationService.notifyReservationReady(res);
        assertThat(notificationRepository.findByUser_IdOrderByCreatedAtDescIdDesc(member.getId())).isEmpty();

        // Re-enable website notifications
        mvc.perform(post("/member/notifications/preferences")
                        .with(user(new CustomUserDetails(member)))
                        .with(csrf())
                        .param("websiteEnabled", "true")
                        .param("_websiteEnabled", "on")
                        .param("emailEnabled", "true")
                        .param("_emailEnabled", "on"))
                .andExpect(status().is3xxRedirection());

        notificationService.notifyReservationReady(res);
        assertThat(notificationRepository.findByUser_IdOrderByCreatedAtDescIdDesc(member.getId())).hasSize(1);
    }

    @Test
    void automatedReminderGenerationIsIdempotentPerDayAndRespectsSecurity() throws Exception {
        loanRepository.saveAndFlush(Loan.builder()
                .copy(copyDueSoon)
                .member(member)
                .dueDate(LocalDate.now().plusDays(2))
                .status(LoanStatus.BORROWING)
                .build());
        loanRepository.saveAndFlush(Loan.builder()
                .copy(copyOverdue)
                .member(member)
                .dueDate(LocalDate.now().minusDays(3))
                .status(LoanStatus.OVERDUE)
                .build());

        int firstRun = notificationService.generateAutomatedLoanReminders();
        int secondRun = notificationService.generateAutomatedLoanReminders();

        assertThat(firstRun).isEqualTo(2);
        assertThat(secondRun).isZero();

        List<Notification> inbox = notificationRepository.findByUser_IdOrderByCreatedAtDescIdDesc(member.getId());
        assertThat(inbox).extracting(Notification::getType)
                .containsExactlyInAnyOrder(NotificationType.DUE_SOON, NotificationType.OVERDUE);

        // Staff & anonymous rejected on /member/notifications
        mvc.perform(get("/member/notifications")).andExpect(status().is3xxRedirection());
        mvc.perform(get("/member/notifications").with(user(new CustomUserDetails(librarian))))
                .andExpect(status().isForbidden());
    }
}
