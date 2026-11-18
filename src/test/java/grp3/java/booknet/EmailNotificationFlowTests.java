package grp3.java.booknet;

import grp3.java.booknet.dto.notification.EmailComposerView;
import grp3.java.booknet.dto.notification.EmailSendForm;
import grp3.java.booknet.entity.book.Book;
import grp3.java.booknet.entity.book.BookCopy;
import grp3.java.booknet.entity.book_service.Loan;
import grp3.java.booknet.entity.notification.EmailLog;
import grp3.java.booknet.entity.notification.NotificationPreference;
import grp3.java.booknet.entity.user.MemberCard;
import grp3.java.booknet.entity.user.User;
import grp3.java.booknet.enums.book.CopyStatus;
import grp3.java.booknet.enums.book.PublicationStatus;
import grp3.java.booknet.enums.book_service.LoanStatus;
import grp3.java.booknet.enums.notification.EmailLogStatus;
import grp3.java.booknet.enums.notification.EmailTemplateType;
import grp3.java.booknet.enums.user.AuthProvider;
import grp3.java.booknet.enums.user.UserRole;
import grp3.java.booknet.enums.user.UserStatus;
import grp3.java.booknet.exception.account.AccountException;
import grp3.java.booknet.repository.book.BookCopyRepository;
import grp3.java.booknet.repository.book.BookRepository;
import grp3.java.booknet.repository.book_service.LoanRepository;
import grp3.java.booknet.repository.book_service.ReservationRepository;
import grp3.java.booknet.repository.notification.EmailLogRepository;
import grp3.java.booknet.repository.notification.EmailTemplateRepository;
import grp3.java.booknet.repository.notification.NotificationPreferenceRepository;
import grp3.java.booknet.repository.notification.NotificationRepository;
import grp3.java.booknet.repository.user.UserRepository;
import grp3.java.booknet.security.dto.CustomUserDetails;
import grp3.java.booknet.service.notification.EmailNotificationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EmailNotificationFlowTests {

    @MockitoBean
    private JavaMailSender mailSender;

    @Autowired private MockMvc mvc;
    @Autowired private EmailNotificationService emailNotificationService;
    @Autowired private EmailLogRepository emailLogRepository;
    @Autowired private EmailTemplateRepository emailTemplateRepository;
    @Autowired private NotificationPreferenceRepository preferenceRepository;
    @Autowired private NotificationRepository notificationRepository;
    @Autowired private LoanRepository loanRepository;
    @Autowired private ReservationRepository reservationRepository;
    @Autowired private BookCopyRepository bookCopyRepository;
    @Autowired private BookRepository bookRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private User librarian;
    private User memberOverdue;
    private User memberOptedOut;
    private Book book;

    @BeforeEach
    void setUp() {
        reset(mailSender);
        cleanAll();

        librarian = userRepository.saveAndFlush(User.builder()
                .fullName("Librarian Hoa")
                .email("hoa.email@booknet.test")
                .password(passwordEncoder.encode(TestPasswords.valid()))
                .role(UserRole.ROLE_LIBRARIAN)
                .provider(AuthProvider.LOCAL)
                .enabled(true)
                .emailVerified(true)
                .status(UserStatus.ACTIVE)
                .build());

        memberOverdue = saveMember("Minh Anh", "minhanh.mail@booknet.test", "BN-M01");
        memberOptedOut = saveMember("Opt Out Reader", "optout.mail@booknet.test", "BN-M02");

        preferenceRepository.saveAndFlush(NotificationPreference.builder()
                .user(memberOptedOut)
                .websiteEnabled(true)
                .emailEnabled(false)
                .eventsNewsletterEnabled(false)
                .build());

        book = bookRepository.saveAndFlush(Book.builder()
                .bookCode("BK-MAIL-01")
                .isbn("978-0134685991")
                .title("Effective Java")
                .publicationStatus(PublicationStatus.PUBLISHED)
                .deleted(false)
                .build());

        BookCopy copy1 = bookCopyRepository.saveAndFlush(BookCopy.builder()
                .book(book).barcode("EJ-MAIL-01").status(CopyStatus.BORROWED).build());
        BookCopy copy2 = bookCopyRepository.saveAndFlush(BookCopy.builder()
                .book(book).barcode("EJ-MAIL-02").status(CopyStatus.BORROWED).build());
        BookCopy copy3 = bookCopyRepository.saveAndFlush(BookCopy.builder()
                .book(book).barcode("EJ-MAIL-03").status(CopyStatus.BORROWED).build());

        // Overdue loan 1 for memberOverdue
        loanRepository.saveAndFlush(Loan.builder()
                .copy(copy1).member(memberOverdue).dueDate(LocalDate.now().minusDays(4)).status(LoanStatus.OVERDUE).build());
        // Second overdue loan for same email -> verifies deduplication in GROUP mode
        loanRepository.saveAndFlush(Loan.builder()
                .copy(copy2).member(memberOverdue).dueDate(LocalDate.now().minusDays(2)).status(LoanStatus.OVERDUE).build());
        // Overdue loan for opted-out member -> verifies skipped opt-out in GROUP mode
        loanRepository.saveAndFlush(Loan.builder()
                .copy(copy3).member(memberOptedOut).dueDate(LocalDate.now().minusDays(3)).status(LoanStatus.OVERDUE).build());
    }

    @AfterEach
    void tearDown() {
        cleanAll();
    }

    private void cleanAll() {
        emailLogRepository.deleteAll();
        emailTemplateRepository.deleteAll();
        notificationRepository.deleteAll();
        preferenceRepository.deleteAll();
        loanRepository.deleteAll();
        reservationRepository.deleteAll();
        bookCopyRepository.deleteAll();
        bookRepository.deleteAll();
        userRepository.deleteAll();
    }

    private User saveMember(String name, String email, String cardNumber) {
        User u = User.builder()
                .fullName(name)
                .email(email)
                .password(passwordEncoder.encode(TestPasswords.valid()))
                .memberCode(cardNumber)
                .role(UserRole.ROLE_USER)
                .provider(AuthProvider.LOCAL)
                .enabled(true)
                .emailVerified(true)
                .status(UserStatus.ACTIVE)
                .build();
        MemberCard card = new MemberCard();
        card.setUser(u);
        card.setCardNumber(cardNumber);
        card.setIssuedOn(LocalDate.now().minusDays(10));
        card.setExpiresOn(LocalDate.now().plusYears(1));
        u.setMemberCard(card);
        return userRepository.saveAndFlush(u);
    }

    @Test
    void securityEnforcesServiceEmailAcrossAliasesAndSidebarLinkIsActive() throws Exception {
        for (String role : List.of("ROLE_USER", "ROLE_LIBRARIAN", "ROLE_ADMIN")) {
            for (String alias : List.of("/staff/notifications/email", "/librarian/notifications/email", "/admin/notifications/email")) {
                var principal = user("no-email-cap@booknet.test")
                        .authorities(new SimpleGrantedAuthority(role), new SimpleGrantedAuthority("MEMBER_READ"));
                mvc.perform(get(alias).with(principal)).andExpect(status().isForbidden());
                mvc.perform(post(alias).with(principal).with(csrf())).andExpect(status().isForbidden());
            }
        }

        mvc.perform(get("/staff/notifications/email").with(user(new CustomUserDetails(librarian))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Send email notifications")))
                .andExpect(content().string(containsString("admin-sidebar")))
                .andExpect(content().string(containsString("Recent sending log")))
                .andExpect(content().string(not(containsString(" th:"))));
    }

    @Test
    void templateValidationPreviewSaveAndResetWorkCorrectly() throws Exception {
        // Unknown placeholder fails validation and blocks dispatch
        EmailSendForm badForm = EmailSendForm.builder()
                .templateType(EmailTemplateType.OVERDUE)
                .recipientMode("SPECIFIC")
                .selectedMemberId(memberOverdue.getId())
                .subjectTemplate("Hello {{memberName}}")
                .bodyTemplate("Invalid variable {{unknownToken}} inside message")
                .build();

        EmailComposerView preview = emailNotificationService.getComposerView(badForm, true);
        assertThat(preview.preview().valid()).isFalse();
        assertThat(preview.preview().unknownVariables()).contains("{{unknownToken}}");

        assertThatThrownBy(() -> emailNotificationService.sendEmails(badForm, librarian.getEmail()))
                .isInstanceOf(AccountException.class)
                .hasMessageContaining("Unknown variable");

        // Save custom template with Figma alias variables & reset to default
        mvc.perform(post("/staff/notifications/email")
                        .with(user(new CustomUserDetails(librarian)))
                        .with(csrf())
                        .param("action", "save-template")
                        .param("templateType", "DUE_SOON")
                        .param("subjectTemplate", "[booknet] Custom {{ten_sach}}")
                        .param("bodyTemplate", "Xin chào {{ten_thanh_vien}}, hạn trả {{han_tra}}."))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/staff/notifications/email?templateType=DUE_SOON"));

        assertThat(emailNotificationService.getTemplate(EmailTemplateType.DUE_SOON).getSubjectTemplate())
                .isEqualTo("[booknet] Custom {{ten_sach}}");

        mvc.perform(post("/staff/notifications/email")
                        .with(user(new CustomUserDetails(librarian)))
                        .with(csrf())
                        .param("action", "reset-template")
                        .param("templateType", "DUE_SOON"))
                .andExpect(status().is3xxRedirection());

        assertThat(emailNotificationService.getTemplate(EmailTemplateType.DUE_SOON).getSubjectTemplate())
                .contains("{{bookTitle}}");
    }

    @Test
    void reviewAndGroupSendSendsIndividualMessagesDeduplicatesOptedOutAndPreventsDuplicateSubmit() throws Exception {
        String idempToken = "batch-test-001";

        // Review confirmation step renders confirmation box
        mvc.perform(post("/staff/notifications/email")
                        .with(user(new CustomUserDetails(librarian)))
                        .with(csrf())
                        .param("action", "review")
                        .param("templateType", "OVERDUE")
                        .param("recipientMode", "GROUP")
                        .param("subjectTemplate", "[booknet] Overdue {{bookTitle}} ({{daysOverdue}}d)")
                        .param("bodyTemplate", "Hi {{memberName}}, {{bookTitle}} is {{daysOverdue}} days overdue.")
                        .param("idempotencyToken", idempToken))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("confirm-dispatch-box")))
                .andExpect(content().string(containsString("Confirm &amp; send emails")));

        // Actual send dispatch
        mvc.perform(post("/staff/notifications/email")
                        .with(user(new CustomUserDetails(librarian)))
                        .with(csrf())
                        .param("templateType", "OVERDUE")
                        .param("recipientMode", "GROUP")
                        .param("subjectTemplate", "[booknet] Overdue {{bookTitle}} ({{daysOverdue}}d)")
                        .param("bodyTemplate", "Hi {{memberName}}, {{bookTitle}} is {{daysOverdue}} days overdue.")
                        .param("idempotencyToken", idempToken))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/staff/notifications/email?templateType=OVERDUE"));

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender, times(1)).send(captor.capture());

        SimpleMailMessage sentMsg = captor.getValue();
        assertThat(sentMsg.getTo()).containsExactly(memberOverdue.getEmail());
        assertThat(sentMsg.getSubject()).isEqualTo("[booknet] Overdue Effective Java (4d)");
        assertThat(sentMsg.getText()).contains("Minh Anh").contains("4 days overdue");

        List<EmailLog> logs = emailLogRepository.findTop50ByOrderByCreatedAtDescIdDesc();
        assertThat(logs).hasSize(1);
        assertThat(logs.get(0).getStatus()).isEqualTo(EmailLogStatus.SENT);

        // Resubmit same idempotency token -> skipped cleanly without calling mailSender again
        mvc.perform(post("/staff/notifications/email")
                        .with(user(new CustomUserDetails(librarian)))
                        .with(csrf())
                        .param("templateType", "OVERDUE")
                        .param("recipientMode", "GROUP")
                        .param("subjectTemplate", "[booknet] Overdue {{bookTitle}} ({{daysOverdue}}d)")
                        .param("bodyTemplate", "Hi {{memberName}}, {{bookTitle}} is {{daysOverdue}} days overdue.")
                        .param("idempotencyToken", idempToken))
                .andExpect(status().is3xxRedirection());

        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
    }

    @Test
    void failedSmtpDeliveryRecordsFailureAndRetryRecoversIdempotently() throws Exception {
        doThrow(new MailSendException("SMTP connection refused"))
                .when(mailSender).send(any(SimpleMailMessage.class));

        mvc.perform(post("/staff/notifications/email")
                        .with(user(new CustomUserDetails(librarian)))
                        .with(csrf())
                        .param("templateType", "OVERDUE")
                        .param("recipientMode", "SPECIFIC")
                        .param("selectedMemberId", memberOverdue.getId().toString())
                        .param("subjectTemplate", "[booknet] Overdue {{bookTitle}}")
                        .param("bodyTemplate", "Dear {{memberName}}, please return {{bookTitle}}.")
                        .param("idempotencyToken", "retry-batch-001"))
                .andExpect(status().is3xxRedirection());

        EmailLog failedLog = emailLogRepository.findTop50ByOrderByCreatedAtDescIdDesc().get(0);
        assertThat(failedLog.getStatus()).isEqualTo(EmailLogStatus.FAILED);
        assertThat(failedLog.getErrorMessage()).contains("SMTP connection refused");
        assertThat(failedLog.getRetryCount()).isZero();

        mvc.perform(get("/staff/notifications/email").with(user(new CustomUserDetails(librarian))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Send failed")))
                .andExpect(content().string(containsString("Retry")));

        // Recover SMTP and retry failed log
        reset(mailSender);
        mvc.perform(post("/staff/notifications/email/logs/" + failedLog.getId() + "/retry")
                        .with(user(new CustomUserDetails(librarian)))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection());

        EmailLog retriedLog = emailLogRepository.findById(failedLog.getId()).orElseThrow();
        assertThat(retriedLog.getStatus()).isEqualTo(EmailLogStatus.SENT);
        assertThat(retriedLog.getRetryCount()).isEqualTo(1);
        assertThat(retriedLog.getErrorMessage()).isNull();
        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));

        // Second retry attempt on already SENT log must NOT send again
        mvc.perform(post("/staff/notifications/email/logs/" + failedLog.getId() + "/retry")
                        .with(user(new CustomUserDetails(librarian)))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("errorMessage"));

        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
    }
}
