package grp3.java.booknet;

import grp3.java.booknet.dto.borrowing.BorrowRequestForm;
import grp3.java.booknet.entity.book.Book;
import grp3.java.booknet.entity.book.BookCopy;
import grp3.java.booknet.entity.book_service.BorrowRequest;
import grp3.java.booknet.entity.book_service.BorrowRequestItem;
import grp3.java.booknet.entity.book_service.CirculationPolicy;
import grp3.java.booknet.entity.book_service.Loan;
import grp3.java.booknet.entity.book_service.LoanItem;
import grp3.java.booknet.entity.book_service.Reservation;
import grp3.java.booknet.entity.user.MemberCard;
import grp3.java.booknet.entity.user.User;
import grp3.java.booknet.enums.book.CopyStatus;
import grp3.java.booknet.enums.book.PublicationStatus;
import grp3.java.booknet.enums.book_service.BorrowRequestStatus;
import grp3.java.booknet.enums.book_service.LoanItemStatus;
import grp3.java.booknet.enums.book_service.LoanStatus;
import grp3.java.booknet.enums.book_service.ReservationStatus;
import grp3.java.booknet.enums.book_service.ReturnCondition;
import grp3.java.booknet.enums.user.AuthProvider;
import grp3.java.booknet.enums.user.UserRole;
import grp3.java.booknet.enums.user.UserStatus;
import grp3.java.booknet.exception.account.AccountException;
import grp3.java.booknet.repository.book.BookCopyRepository;
import grp3.java.booknet.repository.book.BookRepository;
import grp3.java.booknet.repository.book_service.BorrowRequestItemRepository;
import grp3.java.booknet.repository.book_service.BorrowRequestRepository;
import grp3.java.booknet.repository.book_service.LoanItemRepository;
import grp3.java.booknet.repository.book_service.LoanRepository;
import grp3.java.booknet.repository.book_service.ReservationRepository;
import grp3.java.booknet.repository.user.UserRepository;
import grp3.java.booknet.service.admin.CirculationPolicyService;
import grp3.java.booknet.service.borrowing.BorrowService;
import grp3.java.booknet.service.borrowing.LoanService;
import grp3.java.booknet.service.borrowing.MemberLoanService;
import grp3.java.booknet.service.catalog.BookDetailsService;
import grp3.java.booknet.service.reservation.ReservationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CirculationPolicyIntegrationTests {

    @Autowired MockMvc mvc;
    @Autowired CirculationPolicyService policyService;
    @Autowired LoanService loanService;
    @Autowired BorrowService borrowService;
    @Autowired MemberLoanService memberLoanService;
    @Autowired ReservationService reservationService;
    @Autowired BookDetailsService bookDetailsService;
    @Autowired UserRepository userRepository;
    @Autowired BookRepository bookRepository;
    @Autowired BookCopyRepository bookCopyRepository;
    @Autowired BorrowRequestRepository borrowRequestRepository;
    @Autowired BorrowRequestItemRepository borrowRequestItemRepository;
    @Autowired LoanRepository loanRepository;
    @Autowired LoanItemRepository loanItemRepository;
    @Autowired ReservationRepository reservationRepository;

    private User librarian;
    private User member;
    private Book book1;
    private Book book2;
    private Book book3;
    private BookCopy copy1;
    private BookCopy copy2;

    @BeforeEach
    void setUp() {
        librarian = userRepository.save(User.builder()
                .fullName("Policy Librarian")
                .email("librarian.policy@booknet.local")
                .password("Password123")
                .role(UserRole.ROLE_ADMIN)
                .status(UserStatus.ACTIVE)
                .enabled(true)
                .emailVerified(true)
                .provider(AuthProvider.LOCAL)
                .memberCode("LIB-POL-01")
                .build());

        member = userRepository.save(User.builder()
                .fullName("Policy Member")
                .email("member.policy@booknet.local")
                .password("Password123")
                .role(UserRole.ROLE_USER)
                .status(UserStatus.ACTIVE)
                .enabled(true)
                .emailVerified(true)
                .provider(AuthProvider.LOCAL)
                .memberCode("MB-POL-01")
                .build());
        MemberCard card = new MemberCard();
        card.setUser(member);
        card.setCardNumber("CARD-POL-01");
        card.setIssuedOn(LocalDate.now().minusDays(2));
        card.setExpiresOn(LocalDate.now().plusYears(1));
        member.setMemberCard(card);
        member = userRepository.saveAndFlush(member);

        book1 = bookRepository.save(Book.builder()
                .title("Policy Book One")
                .publicationStatus(PublicationStatus.PUBLISHED)
                .build());
        book2 = bookRepository.save(Book.builder()
                .title("Policy Book Two")
                .publicationStatus(PublicationStatus.PUBLISHED)
                .build());
        book3 = bookRepository.save(Book.builder()
                .title("Policy Book Three")
                .publicationStatus(PublicationStatus.PUBLISHED)
                .build());

        copy1 = bookCopyRepository.save(BookCopy.builder()
                .book(book1)
                .barcode("BC-POL-01")
                .status(CopyStatus.AVAILABLE)
                .build());
        copy2 = bookCopyRepository.save(BookCopy.builder()
                .book(book2)
                .barcode("BC-POL-02")
                .status(CopyStatus.AVAILABLE)
                .build());
        bookCopyRepository.save(BookCopy.builder()
                .book(book3)
                .barcode("BC-POL-03")
                .status(CopyStatus.AVAILABLE)
                .build());
    }

    @Test
    @DisplayName("Admin settings update enforces quota, loan period, renew period, hold days, and daily fine across services")
    @WithMockUser(username = "librarian.policy@booknet.local", authorities = {"ROLE_ADMIN", "ROLE_LIBRARIAN", "CIRCULATION"})
    void updatedPolicyAppliesToBorrowLoanRenewFineReservationAndViews() throws Exception {
        var adminAuth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        mvc.perform(post("/admin/settings")
                        .with(csrf())
                        .param("maxLoansPerMember", "2")
                        .param("loanPeriodDays", "10")
                        .param("renewPeriodDays", "4")
                        .param("dailyFineRate", "7500")
                        .param("reservationHoldDays", "5"))
                .andExpect(status().is3xxRedirection());
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(adminAuth);

        CirculationPolicy activePolicy = policyService.getPolicy();
        assertThat(activePolicy.getMaxLoansPerMember()).isEqualTo(2);
        assertThat(activePolicy.getLoanPeriodDays()).isEqualTo(10);
        assertThat(activePolicy.getRenewPeriodDays()).isEqualTo(4);
        assertThat(activePolicy.getDailyFineRate()).isEqualByComparingTo(new BigDecimal("7500.00"));
        assertThat(activePolicy.getReservationHoldDays()).isEqualTo(5);

        // 1. Quota & loan period reflected in BorrowService, LoanService lookup, and BookDetailsService
        var patronInfo = loanService.lookupMember(member.getEmail());
        assertThat(patronInfo.getMaxAllowedLoans()).isEqualTo(2);

        var confirmView = borrowService.prepareConfirmView(member.getId(), List.of(book1.getId(), book2.getId(), book3.getId()));
        assertThat(confirmView.maxAllowedLoans()).isEqualTo(2);
        assertThat(confirmView.loanPeriodDays()).isEqualTo(10);
        assertThat(confirmView.renewPeriodDays()).isEqualTo(4);
        assertThat(confirmView.returnDueDate()).isEqualTo(LocalDate.now().plusDays(10));
        assertThat(confirmView.eligibleToBorrow()).isFalse();

        BorrowRequestForm overLimitForm = new BorrowRequestForm();
        overLimitForm.setBookIds(List.of(book1.getId(), book2.getId(), book3.getId()));
        overLimitForm.setTermsAccepted(true);
        assertThatThrownBy(() -> borrowService.submitBorrowRequest(member.getId(), overLimitForm))
                .isInstanceOf(AccountException.class)
                .hasMessageContaining("2");

        assertThat(bookDetailsService.details(book1.getId(), false).dueDate())
                .isEqualTo(LocalDate.now().plusDays(10));

        // 2. Handover uses loanPeriodDays (10), renew uses renewPeriodDays (4), MemberLoansView uses both
        BorrowRequest req = borrowRequestRepository.save(BorrowRequest.builder()
                .requestCode("BR-POL-01")
                .member(member)
                .status(BorrowRequestStatus.APPROVED)
                .build());
        borrowRequestItemRepository.save(BorrowRequestItem.builder().request(req).book(book1).build());

        Loan handedOver = loanService.handoverLoan(req.getId(), List.of("BC-POL-01"), null, librarian.getEmail());
        assertThat(handedOver.getDueDate()).isEqualTo(LocalDate.now().plusDays(10));

        var myBooksBeforeRenew = memberLoanService.getMemberLoansView(member.getId());
        assertThat(myBooksBeforeRenew.maxAllowedLoans()).isEqualTo(2);
        assertThat(myBooksBeforeRenew.activeLoans()).hasSize(1);
        assertThat(myBooksBeforeRenew.activeLoans().get(0).renewalStatusHint()).isEqualTo("Eligible for a 4-day renewal");
        assertThat(myBooksBeforeRenew.renewSuggestion()).isNotNull();
        assertThat(myBooksBeforeRenew.renewSuggestion().renewPeriodDays()).isEqualTo(4);
        assertThat(myBooksBeforeRenew.renewSuggestion().estimatedNewDueDate()).isEqualTo(LocalDate.now().plusDays(14));

        LoanItem item = loanItemRepository.findByLoan_Id(handedOver.getId()).get(0);
        LoanItem renewed = loanService.renewLoanItem(item.getId(), librarian.getEmail());
        assertThat(renewed.getDueDate()).isEqualTo(LocalDate.now().plusDays(14));

        // 3. Overdue return in LoanService uses dailyFineRate (7500/day * 3 days = 22500)
        Loan overdueLoan = loanRepository.save(Loan.builder()
                .copy(copy2)
                .member(member)
                .dueDate(LocalDate.now().minusDays(3))
                .status(LoanStatus.BORROWING)
                .build());
        copy2.setStatus(CopyStatus.BORROWED);
        bookCopyRepository.save(copy2);
        loanItemRepository.save(LoanItem.builder()
                .loan(overdueLoan)
                .copy(copy2)
                .status(LoanItemStatus.BORROWING)
                .dueDate(LocalDate.now().minusDays(3))
                .build());

        var receipt = loanService.receiveReturn("BC-POL-02", ReturnCondition.GOOD, "Late", librarian.getEmail());
        assertThat(receipt.getTotalFines()).isEqualByComparingTo(new BigDecimal("22500.00"));

        // 4. Reservation hold uses reservationHoldDays (5)
        Reservation pending = reservationRepository.save(Reservation.builder()
                .member(member)
                .book(book3)
                .status(ReservationStatus.PENDING)
                .reservedAt(LocalDateTime.now())
                .build());
        Reservation ready = reservationService.assignCopyAndMarkReady(pending.getId(), librarian.getEmail());
        assertThat(ready.getPickupUntil()).isBetween(
                LocalDateTime.now().plusDays(5).minusMinutes(2),
                LocalDateTime.now().plusDays(5).plusMinutes(2));

        // 5. Rendered views show updated numbers
        mvc.perform(get("/staff/loans/receive-return"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("10-day loan")));

        var memberPrincipal = new grp3.java.booknet.security.dto.CustomUserDetails(member);
        mvc.perform(get("/loans/confirm").param("bookId", book1.getId().toString()).with(user(memberPrincipal)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("10-day loan")))
                .andExpect(content().string(containsString("4 more days")))
                .andExpect(content().string(containsString("Up to 2 books at a time")));

        mvc.perform(get("/member/reservations").with(user(memberPrincipal)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Held for 5 days (120 hours)")))
                .andExpect(content().string(containsString("10-day borrowing period")));
    }
}
