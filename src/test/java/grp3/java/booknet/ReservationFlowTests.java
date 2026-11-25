package grp3.java.booknet;

import grp3.java.booknet.entity.book.Author;
import grp3.java.booknet.entity.book.Book;
import grp3.java.booknet.entity.book.BookCopy;
import grp3.java.booknet.entity.book.Category;
import grp3.java.booknet.entity.book_service.Loan;
import grp3.java.booknet.entity.book_service.LoanItem;
import grp3.java.booknet.entity.book_service.Reservation;
import grp3.java.booknet.entity.notification.Notification;
import grp3.java.booknet.entity.user.MemberCard;
import grp3.java.booknet.entity.user.User;
import grp3.java.booknet.enums.book.CopyStatus;
import grp3.java.booknet.enums.book.PublicationStatus;
import grp3.java.booknet.enums.book_service.LoanItemStatus;
import grp3.java.booknet.enums.book_service.LoanStatus;
import grp3.java.booknet.enums.book_service.ReservationStatus;
import grp3.java.booknet.enums.book_service.ReturnCondition;
import grp3.java.booknet.enums.notification.NotificationType;
import grp3.java.booknet.enums.user.AuthProvider;
import grp3.java.booknet.enums.user.UserRole;
import grp3.java.booknet.enums.user.UserStatus;
import grp3.java.booknet.exception.account.AccountException;
import grp3.java.booknet.exception.borrowing.LoanException;
import grp3.java.booknet.exception.reservation.ReservationException;
import grp3.java.booknet.repository.book.AuthorRepository;
import grp3.java.booknet.repository.book.BookCopyRepository;
import grp3.java.booknet.repository.book.BookRepository;
import grp3.java.booknet.repository.book.CategoryRepository;
import grp3.java.booknet.repository.book_service.*;
import grp3.java.booknet.repository.notification.NotificationPreferenceRepository;
import grp3.java.booknet.repository.notification.NotificationRepository;
import grp3.java.booknet.repository.user.UserRepository;
import grp3.java.booknet.security.dto.CustomUserDetails;
import grp3.java.booknet.service.borrowing.MemberLoanService;
import grp3.java.booknet.service.borrowing.LoanService;
import grp3.java.booknet.service.reservation.ReservationQueueService;
import grp3.java.booknet.service.reservation.ReservationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
class ReservationFlowTests {

    @MockitoBean
    private JavaMailSender mailSender;

    @Autowired private MockMvc mvc;
    @Autowired private ReservationService reservationService;
    @Autowired private ReservationQueueService reservationQueueService;
    @Autowired private LoanService loanService;
    @Autowired private MemberLoanService memberLoanService;

    @Autowired private NotificationRepository notificationRepository;
    @Autowired private NotificationPreferenceRepository notificationPreferenceRepository;
    @Autowired private ReturnReceiptItemRepository returnReceiptItemRepository;
    @Autowired private ReturnReceiptRepository returnReceiptRepository;
    @Autowired private FineRepository fineRepository;
    @Autowired private LoanItemRepository loanItemRepository;
    @Autowired private BorrowRequestItemRepository borrowRequestItemRepository;
    @Autowired private BorrowRequestRepository borrowRequestRepository;
    @Autowired private LoanRepository loanRepository;
    @Autowired private ReservationRepository reservationRepository;
    @Autowired private BookCopyRepository bookCopyRepository;
    @Autowired private BookRepository bookRepository;
    @Autowired private AuthorRepository authorRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private User memberA;
    private User memberB;
    private User memberC;
    private User librarian;
    private Book book;
    private BookCopy copy1;
    private Loan borrowerLoan;

    @BeforeEach
    void setUp() {
        cleanAll();
        TestMail.prepare(mailSender);

        memberA = createMember("Minh Anh", "minhanh.resv@booknet.test", "BN-2026-0148", true);
        memberB = createMember("Binh Tran", "binh.borrower@booknet.test", "BN-2026-0149", true);
        memberC = createMember("Chi Nguyen", "chi.resv@booknet.test", "BN-2026-0150", true);

        librarian = userRepository.saveAndFlush(User.builder()
                .fullName("Librarian Lan")
                .email("lan.librarian@booknet.test")
                .password(passwordEncoder.encode(TestPasswords.valid()))
                .role(UserRole.ROLE_LIBRARIAN)
                .provider(AuthProvider.LOCAL)
                .enabled(true)
                .emailVerified(true)
                .status(UserStatus.ACTIVE)
                .build());

        Author author = authorRepository.saveAndFlush(Author.builder().name("Robert C. Martin").build());
        Category category = categoryRepository.saveAndFlush(Category.builder().name("Software Craft").build());

        book = bookRepository.saveAndFlush(Book.builder()
                .bookCode("BK-CLEAN-01")
                .isbn("978-0132350884")
                .title("Clean Code")
                .publicationStatus(PublicationStatus.PUBLISHED)
                .deleted(false)
                .authors(Set.of(author))
                .categories(Set.of(category))
                .build());

        copy1 = bookCopyRepository.saveAndFlush(BookCopy.builder()
                .book(book)
                .barcode("CC-2026-001")
                .shelfLocation("Floor 1 · Shelf A3")
                .status(CopyStatus.BORROWED)
                .build());

        borrowerLoan = loanRepository.saveAndFlush(Loan.builder()
                .copy(copy1)
                .member(memberB)
                .issuedBy(librarian)
                .borrowedAt(LocalDateTime.now().minusDays(9))
                .dueDate(LocalDate.now().plusDays(5))
                .status(LoanStatus.BORROWING)
                .renewCount(0)
                .build());

        loanItemRepository.saveAndFlush(LoanItem.builder()
                .loan(borrowerLoan)
                .copy(copy1)
                .dueDate(borrowerLoan.getDueDate())
                .status(LoanItemStatus.BORROWING)
                .renewCount(0)
                .build());
    }

    @AfterEach
    void tearDown() {
        cleanAll();
    }

    private void cleanAll() {
        notificationRepository.deleteAll();
        notificationPreferenceRepository.deleteAll();
        returnReceiptItemRepository.deleteAll();
        returnReceiptRepository.deleteAll();
        fineRepository.deleteAll();
        loanItemRepository.deleteAll();
        borrowRequestItemRepository.deleteAll();
        borrowRequestRepository.deleteAll();
        loanRepository.deleteAll();
        reservationRepository.deleteAll();
        bookCopyRepository.deleteAll();
        bookRepository.deleteAll();
        authorRepository.deleteAll();
        categoryRepository.deleteAll();
        userRepository.deleteAll();
    }

    private User createMember(String name, String email, String cardNumber, boolean activeCard) {
        User u = User.builder()
                .fullName(name)
                .email(email)
                .username(email.substring(0, email.indexOf('@')))
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
        card.setIssuedOn(LocalDate.now().minusDays(30));
        card.setExpiresOn(activeCard ? LocalDate.now().plusYears(1) : LocalDate.now().minusDays(1));
        u.setMemberCard(card);
        return userRepository.saveAndFlush(u);
    }

    @Test
    void memberCanReserveFromCatalogAndSeeLiveFifoQueueAndCancelFlow() throws Exception {
        mvc.perform(get("/catalog").with(user(new CustomUserDetails(memberA))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Reserve book")))
                .andExpect(content().string(not(containsString(" th:"))));

        mvc.perform(get("/catalog/" + book.getId()).with(user(new CustomUserDetails(memberA))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Reserve book")))
                .andExpect(content().string(not(containsString(" th:"))));

        mvc.perform(post("/member/reservations")
                        .with(user(new CustomUserDetails(memberA)))
                        .with(csrf())
                        .param("bookId", book.getId().toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/member/reservations"));

        mvc.perform(get("/catalog/" + book.getId()).with(user(new CustomUserDetails(memberA))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("In your queue")));

        mvc.perform(post("/member/reservations")
                        .with(user(new CustomUserDetails(memberC)))
                        .with(csrf())
                        .param("bookId", book.getId().toString()))
                .andExpect(status().is3xxRedirection());

        mvc.perform(get("/member/reservations").with(user(new CustomUserDetails(memberA))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Clean Code")))
                .andExpect(content().string(containsString("Waiting for a copy")))
                .andExpect(content().string(containsString("01 / 02 readers")))
                .andExpect(content().string(not(containsString(" th:"))));

        mvc.perform(get("/member/reservations").with(user(new CustomUserDetails(memberC))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("02 / 02 readers")));

        mvc.perform(get("/catalog").with(user(new CustomUserDetails(memberA))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("In your queue")));

        // Cancel confirmation card via GET ?cancelId and POST cancel
        Reservation resA = reservationRepository.findByMember_IdOrderByReservedAtDescIdDesc(memberA.getId()).get(0);
        mvc.perform(get("/member/reservations")
                        .with(user(new CustomUserDetails(memberA)))
                        .param("cancelId", resA.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("CONFIRM RESERVATION CANCELLATION")));

        mvc.perform(post("/member/reservations/" + resA.getId() + "/cancel")
                        .with(user(new CustomUserDetails(memberA)))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/member/reservations"));

        Reservation updatedA = reservationRepository.findById(resA.getId()).orElseThrow();
        assertThat(updatedA.getStatus()).isEqualTo(ReservationStatus.CANCELLED);

        mvc.perform(get("/member/reservations").with(user(new CustomUserDetails(memberC))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("01 / 01 readers")));
    }

    @Test
    void validationPreventsDuplicateLoanAvailableBookExpiredCardAndMaxLimit() throws Exception {
        // Active borrower cannot reserve same book
        assertThatThrownBy(() -> reservationService.reserveBook(memberB.getId(), book.getId()))
                .isInstanceOf(ReservationException.class)
                .hasMessageContaining("borrowing");

        // Expired card cannot reserve
        User expiredMember = createMember("Expired Card", "expired@booknet.test", "BN-2026-9999", false);
        assertThatThrownBy(() -> reservationService.reserveBook(expiredMember.getId(), book.getId()))
                .isInstanceOf(ReservationException.class)
                .hasMessageContaining("active");

        // Member lacking RESERVE capability gets 403 via controller
        mvc.perform(post("/member/reservations")
                        .with(user(memberA.getEmail()).authorities(new SimpleGrantedAuthority("ROLE_USER")))
                        .with(csrf())
                        .param("bookId", book.getId().toString()))
                .andExpect(status().isForbidden());

        // Duplicate reservation rejected; max 5 active reservations enforced
        reservationService.reserveBook(memberA.getId(), book.getId());
        assertThatThrownBy(() -> reservationService.reserveBook(memberA.getId(), book.getId()))
                .isInstanceOf(ReservationException.class)
                .hasMessageContaining("already have an active reservation");

        for (int i = 2; i <= 5; i++) {
            Book b = bookRepository.saveAndFlush(Book.builder()
                    .bookCode("BK-EXTRA-" + i)
                    .isbn("978-000000000" + i)
                    .title("Extra Book " + i)
                    .publicationStatus(PublicationStatus.PUBLISHED)
                    .deleted(false)
                    .build());
            bookCopyRepository.saveAndFlush(BookCopy.builder()
                    .book(b).barcode("EX-0" + i).status(CopyStatus.BORROWED).build());
            reservationService.reserveBook(memberA.getId(), b.getId());
        }

        Book sixth = bookRepository.saveAndFlush(Book.builder()
                .bookCode("BK-EXTRA-6").isbn("978-0000000006").title("Sixth Book")
                .publicationStatus(PublicationStatus.PUBLISHED).deleted(false).build());
        bookCopyRepository.saveAndFlush(BookCopy.builder()
                .book(sixth).barcode("EX-06").status(CopyStatus.BORROWED).build());
        assertThatThrownBy(() -> reservationService.reserveBook(memberA.getId(), sixth.getId()))
                .isInstanceOf(ReservationException.class)
                .hasMessageContaining("maximum of 5");

        // Book with available copy cannot be reserved
        Book availBook = bookRepository.saveAndFlush(Book.builder()
                .bookCode("BK-AVAIL").isbn("978-1111111111").title("Available Book")
                .publicationStatus(PublicationStatus.PUBLISHED).deleted(false).build());
        bookCopyRepository.saveAndFlush(BookCopy.builder()
                .book(availBook).barcode("AV-01").status(CopyStatus.AVAILABLE).build());
        assertThatThrownBy(() -> reservationService.reserveBook(memberC.getId(), availBook.getId()))
                .isInstanceOf(ReservationException.class)
                .hasMessageContaining("available");
    }

    @Test
    @WithMockUser(username = "lan.librarian@booknet.test", authorities = {"ROLE_LIBRARIAN", "CIRCULATION"})
    void returningBookBlocksRenewalWhenQueuePresentHoldsForFirstAndAdvancesOnCancel() {
        Reservation resA = reservationService.reserveBook(memberA.getId(), book.getId());
        Reservation resC = reservationService.reserveBook(memberC.getId(), book.getId());

        // Renewal must be blocked while queue exists (both librarian and member flows)
        LoanItem item = loanItemRepository.findAll().get(0);
        assertThatThrownBy(() -> loanService.renewLoanItem(item.getId(), librarian.getEmail()))
                .isInstanceOf(LoanException.class)
                .hasMessageContaining("reservation queue");

        assertThatThrownBy(() -> memberLoanService.requestRenewLoan(memberB.getId(), borrowerLoan.getId()))
                .isInstanceOf(AccountException.class)
                .hasMessageContaining("reserved it");

        // Return copy -> holds for memberA (READY), copy becomes RESERVED, memberA gets RESERVATION_READY notification
        loanService.receiveReturn(copy1.getBarcode(), ReturnCondition.GOOD, "Good condition", librarian.getEmail());

        Reservation updatedA = reservationRepository.findById(resA.getId()).orElseThrow();
        Reservation stillPendingC = reservationRepository.findById(resC.getId()).orElseThrow();
        BookCopy updatedCopy = bookCopyRepository.findById(copy1.getId()).orElseThrow();

        assertThat(updatedA.getStatus()).isEqualTo(ReservationStatus.READY);
        assertThat(updatedA.getAssignedCopy().getId()).isEqualTo(copy1.getId());
        assertThat(updatedA.getPickupUntil()).isAfter(LocalDateTime.now().plusHours(24));
        assertThat(stillPendingC.getStatus()).isEqualTo(ReservationStatus.PENDING);
        assertThat(updatedCopy.getStatus()).isEqualTo(CopyStatus.RESERVED);

        List<Notification> notifsA = notificationRepository.findByUser_IdOrderByCreatedAtDescIdDesc(memberA.getId());
        assertThat(notifsA).anyMatch(n -> n.getType() == NotificationType.RESERVATION_READY);

        // When memberA cancels READY hold, copy immediately transfers to memberC!
        reservationService.cancelMemberReservation(memberA.getId(), resA.getId());

        Reservation canceledA = reservationRepository.findById(resA.getId()).orElseThrow();
        Reservation promotedC = reservationRepository.findById(resC.getId()).orElseThrow();
        assertThat(canceledA.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
        assertThat(promotedC.getStatus()).isEqualTo(ReservationStatus.READY);
        assertThat(promotedC.getAssignedCopy().getId()).isEqualTo(copy1.getId());
        assertThat(bookCopyRepository.findById(copy1.getId()).orElseThrow().getStatus()).isEqualTo(CopyStatus.RESERVED);

        // When memberC cancels last READY hold, copy is released back to AVAILABLE
        reservationService.cancelMemberReservation(memberC.getId(), resC.getId());
        assertThat(bookCopyRepository.findById(copy1.getId()).orElseThrow().getStatus()).isEqualTo(CopyStatus.AVAILABLE);
    }

    @Test
    void expiredReadyHoldAdvancesQueueAndNotifiesExpiredMember() throws Exception {
        Reservation resA = reservationService.reserveBook(memberA.getId(), book.getId());
        Reservation resC = reservationService.reserveBook(memberC.getId(), book.getId());

        // Member B confirms return via MemberLoanService -> holds copy for memberA
        memberLoanService.confirmReturn(librarian.getId(), borrowerLoan.getId(), "Returned cleanly");
        Reservation readyA = reservationRepository.findById(resA.getId()).orElseThrow();
        assertThat(readyA.getStatus()).isEqualTo(ReservationStatus.READY);

        // Simulate expired hold (past 2-day pickup window)
        readyA.setPickupUntil(LocalDateTime.now().minusMinutes(10));
        reservationRepository.saveAndFlush(readyA);

        mvc.perform(post("/staff/reservations/expire-holds")
                        .with(user(new CustomUserDetails(librarian)))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/staff/reservations?tab=ready"));

        Reservation expiredA = reservationRepository.findById(resA.getId()).orElseThrow();
        Reservation readyC = reservationRepository.findById(resC.getId()).orElseThrow();
        assertThat(expiredA.getStatus()).isEqualTo(ReservationStatus.EXPIRED);
        assertThat(expiredA.getExpiredAt()).isNotNull();
        assertThat(readyC.getStatus()).isEqualTo(ReservationStatus.READY);
        assertThat(readyC.getAssignedCopy().getId()).isEqualTo(copy1.getId());

        List<Notification> notifsA = notificationRepository.findByUser_IdOrderByCreatedAtDescIdDesc(memberA.getId());
        assertThat(notifsA).anyMatch(n -> n.getType() == NotificationType.RESERVATION_EXPIRED);
    }

    @Test
    void adminAddingOrRestoringCopyAutoAssignsToPendingQueue() throws Exception {
        Reservation resA = reservationService.reserveBook(memberA.getId(), book.getId());

        mvc.perform(post("/admin/books/" + book.getId() + "/copies/create")
                        .with(user(librarian.getEmail()).authorities(
                                new SimpleGrantedAuthority("ROLE_LIBRARIAN"),
                                new SimpleGrantedAuthority("COPY_MANAGE")))
                        .with(csrf())
                        .param("barcode", "CC-2026-NEW")
                        .param("shelfLocation", "Floor 2 · B1")
                        .param("condition", "New copy"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/books/" + book.getId() + "/edit"));

        Reservation readyA = reservationRepository.findById(resA.getId()).orElseThrow();
        assertThat(readyA.getStatus()).isEqualTo(ReservationStatus.READY);
        BookCopy heldCopy = bookCopyRepository.findById(readyA.getAssignedCopy().getId()).orElseThrow();
        assertThat(heldCopy.getBarcode()).isEqualTo("CC-2026-NEW");
        assertThat(heldCopy.getStatus()).isEqualTo(CopyStatus.RESERVED);

        // Member A sees Ready pickup banner in /member/reservations
        mvc.perform(get("/member/reservations").with(user(new CustomUserDetails(memberA))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("A book is waiting for you")))
                .andExpect(content().string(containsString("Ready for pickup")))
                .andExpect(content().string(containsString("CC-2026-NEW")));

        // Restoring a DAMAGED copy to AVAILABLE also holds it for next pending reader
        Reservation resC = reservationService.reserveBook(memberC.getId(), book.getId());
        BookCopy damagedCopy = bookCopyRepository.saveAndFlush(BookCopy.builder()
                .book(book).barcode("CC-2026-DMG").status(CopyStatus.DAMAGED).build());
        mvc.perform(post("/admin/books/" + book.getId() + "/copies/" + damagedCopy.getId() + "/status")
                        .with(user(librarian.getEmail()).authorities(
                                new SimpleGrantedAuthority("ROLE_LIBRARIAN"),
                                new SimpleGrantedAuthority("COPY_MANAGE")))
                        .with(csrf())
                        .param("targetStatus", "AVAILABLE"))
                .andExpect(status().is3xxRedirection());

        Reservation readyC = reservationRepository.findById(resC.getId()).orElseThrow();
        assertThat(readyC.getStatus()).isEqualTo(ReservationStatus.READY);
        assertThat(bookCopyRepository.findById(damagedCopy.getId()).orElseThrow().getStatus())
                .isEqualTo(CopyStatus.RESERVED);
    }

    @Test
    void librarianDeskAssignsAvailableCopyAndFulfillsIntoLoanWithSecurityChecks() throws Exception {
        Reservation resA = reservationService.reserveBook(memberA.getId(), book.getId());

        // Make an available copy directly in DB to verify staff manual assign + fulfill workflow
        BookCopy secondCopy = bookCopyRepository.saveAndFlush(BookCopy.builder()
                .book(book).barcode("CC-2026-002").shelfLocation("Desk Hold").status(CopyStatus.AVAILABLE).build());

        // Unauthorized member & staff without CIRCULATION denied
        mvc.perform(get("/staff/reservations").with(user(new CustomUserDetails(memberA))))
                .andExpect(status().isForbidden());
        mvc.perform(get("/staff/reservations")
                        .with(user("staff@booknet.test").authorities(
                                new SimpleGrantedAuthority("ROLE_LIBRARIAN"),
                                new SimpleGrantedAuthority("MEMBER_READ"))))
                .andExpect(status().isForbidden());

        // Librarian with CIRCULATION sees pending page across /staff, /librarian, /admin
        mvc.perform(get("/librarian/reservations").with(user(new CustomUserDetails(librarian))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Reservation management")))
                .andExpect(content().string(containsString("Hold available copy")))
                .andExpect(content().string(not(containsString(" th:"))));

        mvc.perform(post("/staff/reservations/" + resA.getId() + "/assign-copy")
                        .with(user(new CustomUserDetails(librarian)))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/staff/reservations?tab=ready"));

        mvc.perform(get("/staff/reservations?tab=ready").with(user(new CustomUserDetails(librarian))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Hand over / Fulfill loan")));

        mvc.perform(post("/staff/reservations/" + resA.getId() + "/fulfill")
                        .with(user(new CustomUserDetails(librarian)))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/staff/reservations?tab=history"));

        Reservation fulfilled = reservationRepository.findById(resA.getId()).orElseThrow();
        BookCopy borrowedCopy = bookCopyRepository.findById(secondCopy.getId()).orElseThrow();
        assertThat(fulfilled.getStatus()).isEqualTo(ReservationStatus.FULFILLED);
        assertThat(fulfilled.getFulfilledAt()).isNotNull();
        assertThat(borrowedCopy.getStatus()).isEqualTo(CopyStatus.BORROWED);

        List<Loan> memberLoans = loanRepository.findByMember_IdOrderByBorrowedAtDesc(memberA.getId());
        assertThat(memberLoans).hasSize(1);
        assertThat(memberLoans.get(0).getStatus()).isEqualTo(LoanStatus.BORROWING);
        assertThat(memberLoans.get(0).getDueDate()).isEqualTo(LocalDate.now().plusDays(14));
    }
}
