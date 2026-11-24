package grp3.java.booknet;

import grp3.java.booknet.dto.loan.*;
import grp3.java.booknet.entity.book.Author;
import grp3.java.booknet.entity.book.Book;
import grp3.java.booknet.entity.book.BookCopy;
import grp3.java.booknet.entity.book.Category;
import grp3.java.booknet.entity.book_service.*;
import grp3.java.booknet.entity.user.User;
import grp3.java.booknet.enums.book.CopyStatus;
import grp3.java.booknet.enums.book_service.*;
import grp3.java.booknet.enums.user.AuthProvider;
import grp3.java.booknet.enums.user.UserRole;
import grp3.java.booknet.enums.user.UserStatus;
import grp3.java.booknet.exception.borrowing.LoanException;
import grp3.java.booknet.repository.book.AuthorRepository;
import grp3.java.booknet.repository.book.BookCopyRepository;
import grp3.java.booknet.repository.book.BookRepository;
import grp3.java.booknet.repository.book.CategoryRepository;
import grp3.java.booknet.repository.book_service.*;
import grp3.java.booknet.repository.user.UserRepository;
import grp3.java.booknet.service.borrowing.LoanService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@WithMockUser(authorities={"ROLE_LIBRARIAN", "CIRCULATION"})
class LoanServiceFlowTests {

    @Autowired MockMvc mvc;
    @Autowired LoanService loanService;
    @Autowired UserRepository userRepository;
    @Autowired BookRepository bookRepository;
    @Autowired BookCopyRepository bookCopyRepository;
    @Autowired AuthorRepository authorRepository;
    @Autowired CategoryRepository categoryRepository;
    @Autowired BorrowRequestRepository borrowRequestRepository;
    @Autowired BorrowRequestItemRepository borrowRequestItemRepository;
    @Autowired LoanRepository loanRepository;
    @Autowired LoanItemRepository loanItemRepository;
    @Autowired ReturnReceiptRepository returnReceiptRepository;
    @Autowired FineRepository fineRepository;
    @Autowired ReservationRepository reservationRepository;

    private User librarian;
    private User member;
    private Book book;
    private BookCopy copy1;
    private BookCopy copy2;

    @BeforeEach
    void setup() {
        librarian = userRepository.save(User.builder()
                .fullName("Trần Thu Hà")
                .email("librarian.flow@booknet.local")
                .password("Password123")
                .role(UserRole.ROLE_ADMIN)
                .status(UserStatus.ACTIVE)
                .provider(AuthProvider.LOCAL)
                .emailVerified(true)
                .memberCode("LIB-001")
                .build());

        member = userRepository.save(User.builder()
                .fullName("Nguyễn Minh Anh")
                .email("member.flow@booknet.local")
                .password("Password123")
                .role(UserRole.ROLE_USER)
                .enabled(true)
                .status(UserStatus.ACTIVE)
                .provider(AuthProvider.LOCAL)
                .emailVerified(true)
                .phone("0912345678")
                .memberCode("MB-FLOW-01")
                .build());

        var card = new grp3.java.booknet.entity.user.MemberCard();
        card.setUser(member);card.setCardNumber("CARD-FLOW-01");card.setIssuedOn(LocalDate.now().minusDays(1));card.setExpiresOn(LocalDate.now().plusYears(1));
        member.setMemberCard(card);userRepository.saveAndFlush(member);

        Author author = authorRepository.save(Author.builder().name("Joshua Bloch").build());
        Category category = categoryRepository.save(Category.builder().name("Software").build());

        book = bookRepository.save(Book.builder()
                .title("Effective Java")
                .isbn("9780134685991")
                .authors(java.util.Set.of(author))
                .categories(java.util.Set.of(category))
                .publishYear((short) 2018)
                .build());

        copy1 = bookCopyRepository.save(BookCopy.builder().book(book).barcode("BC-EJ-01").status(CopyStatus.AVAILABLE).build());
        copy2 = bookCopyRepository.save(BookCopy.builder().book(book).barcode("BC-EJ-02").status(CopyStatus.AVAILABLE).build());
    }

    private BookCopy findCopyByBarcode(String barcode) {
        return bookCopyRepository.findAll().stream()
                .filter(c -> barcode.equalsIgnoreCase(c.getBarcode()))
                .findFirst()
                .orElseThrow();
    }

    @Test
    @DisplayName("Task 4.1: Desk summary correctly tallies pending tasks and active loans")
    void test4_1_LibrarianDeskSummaryCalculation() {
        BorrowRequest r1 = borrowRequestRepository.save(BorrowRequest.builder()
                .requestCode("BR-S1").member(member).status(BorrowRequestStatus.PENDING).build());
        BorrowRequest r2 = borrowRequestRepository.save(BorrowRequest.builder()
                .requestCode("BR-S2").member(member).status(BorrowRequestStatus.APPROVED).build());

        Loan loan = loanRepository.save(Loan.builder()
                .copy(copy1).member(member).dueDate(LocalDate.now().plusDays(10)).status(LoanStatus.BORROWING).build());
        loanItemRepository.save(LoanItem.builder()
                .loan(loan).copy(copy1).status(LoanItemStatus.RETURN_REQUESTED).dueDate(LocalDate.now().plusDays(10)).build());

        LibrarianDeskSummaryDto summary = loanService.getDeskSummary();
        assertThat(summary.getPendingApprovalCount()).isGreaterThanOrEqualTo(1);
        assertThat(summary.getAwaitingHandoverCount()).isGreaterThanOrEqualTo(1);
        assertThat(summary.getPendingReturnsCount()).isGreaterThanOrEqualTo(1);
        assertThat(summary.getTotalPendingTasks()).isGreaterThanOrEqualTo(3);
    }

    @Test
    @DisplayName("Tasks 4.4 & 4.5: Member lookup and borrowing condition verification (<= 5 books, active card, no overdue/fines)")
    void test4_4_and_4_5_MemberLookupAndEligibilityCheck() {
        // 1. Clean member is eligible
        MemberCirculationInfoDto info = loanService.lookupMember("member.flow@booknet.local");
        assertThat(info).isNotNull();
        assertThat(info.isEligible()).isTrue();
        assertThat(info.getActiveLoansCount()).isZero();

        // 2. Member with suspended card is ineligible
        member.setStatus(UserStatus.SUSPENDED);
        userRepository.save(member);
        info = loanService.lookupMember("MB-FLOW-01");
        assertThat(info.isEligible()).isFalse();
        assertThat(info.getIneligibilityReason()).contains("not active");

        // 3. Member with unpaid fines is ineligible
        member.setStatus(UserStatus.ACTIVE);
        userRepository.save(member);
        Loan loan = loanRepository.save(Loan.builder().copy(copy1).member(member).dueDate(LocalDate.now()).status(LoanStatus.BORROWING).build());
        fineRepository.save(Fine.builder().loan(loan).member(member).amount(new BigDecimal("20000.00")).reason(FineReason.OVERDUE).status(FineStatus.UNPAID).build());
        info = loanService.lookupMember("member.flow@booknet.local");
        assertThat(info.isEligible()).isFalse();
        assertThat(info.getIneligibilityReason()).contains("Unpaid fine");
    }

    @Test
    @DisplayName("Task 4.6: Approve borrow request and reject with reason")
    void test4_6_ApproveAndRejectBorrowRequest() {
        BorrowRequest req1 = borrowRequestRepository.save(BorrowRequest.builder()
                .requestCode("BR-REV-01").member(member).status(BorrowRequestStatus.PENDING).build());
        borrowRequestItemRepository.save(BorrowRequestItem.builder().request(req1).book(book).build());

        loanService.approveBorrowRequest(req1.getId(), librarian.getEmail());
        BorrowRequest updated = borrowRequestRepository.findById(req1.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(BorrowRequestStatus.APPROVED);
        assertThat(updated.getApprovedAt()).isNotNull();

        BorrowRequest req2 = borrowRequestRepository.save(BorrowRequest.builder()
                .requestCode("BR-REV-02").member(member).status(BorrowRequestStatus.PENDING).build());
        borrowRequestItemRepository.save(BorrowRequestItem.builder().request(req2).book(book).build());

        loanService.rejectBorrowRequest(req2.getId(), "Card under review", librarian.getEmail());
        BorrowRequest rejected = borrowRequestRepository.findById(req2.getId()).orElseThrow();
        assertThat(rejected.getStatus()).isEqualTo(BorrowRequestStatus.REJECTED);
        assertThat(rejected.getRejectionReason()).isEqualTo("Card under review");
    }

    @Test
    @DisplayName("Tasks 4.7 & 4.8: Hand over physical copies, bind barcodes, and calculate official 14-day checkout")
    void test4_7_and_4_8_HandoverPhysicalCopiesAndOfficialLoanDates() {
        BorrowRequest req = borrowRequestRepository.save(BorrowRequest.builder()
                .requestCode("BR-HO-01").member(member).status(BorrowRequestStatus.APPROVED).build());
        borrowRequestItemRepository.save(BorrowRequestItem.builder().request(req).book(book).build());

        Loan loan = loanService.handoverLoan(req.getId(), List.of("BC-EJ-01"), "Verified reader ID card", librarian.getEmail());
        assertThat(loan).isNotNull();
        assertThat(loan.getId()).isNotNull();
        assertThat(loan.getStatus()).isEqualTo(LoanStatus.BORROWING);
        assertThat(loan.getDueDate()).isEqualTo(LocalDate.now().plusDays(14));

        BookCopy copy = findCopyByBarcode("BC-EJ-01");
        assertThat(copy.getStatus()).isEqualTo(CopyStatus.BORROWED);

        List<LoanItem> items = loanItemRepository.findByLoan_Id(loan.getId());
        assertThat(items).hasSize(1);
        assertThat(items.get(0).getStatus()).isEqualTo(LoanItemStatus.BORROWING);
        assertThat(items.get(0).getDueDate()).isEqualTo(LocalDate.now().plusDays(14));

        Page<BorrowRequestSummaryDto> awaiting = loanService.getApprovedAwaitingHandover(PageRequest.of(0, 10));
        assertThat(awaiting.getContent().stream().noneMatch(r -> r.getRequestId().equals(req.getId()))).isTrue();
    }

    @Test
    @DisplayName("Tasks 4.9 & 4.10: Receive return at desk from pending return request")
    void test4_9_and_4_10_ReceiveReturnAtCounterNormal() {
        Loan loan = loanRepository.save(Loan.builder()
                .copy(copy1).member(member).dueDate(LocalDate.now().plusDays(7)).status(LoanStatus.BORROWING).build());
        copy1.setStatus(CopyStatus.BORROWED);
        bookCopyRepository.save(copy1);
        loanItemRepository.save(LoanItem.builder()
                .loan(loan).copy(copy1).status(LoanItemStatus.RETURN_REQUESTED).dueDate(LocalDate.now().plusDays(7)).build());

        ReturnReceiptViewDto receipt = loanService.receiveReturn("BC-EJ-01", ReturnCondition.GOOD, "Clean copy", librarian.getEmail());
        assertThat(receipt).isNotNull();
        assertThat(receipt.getReceiptCode()).startsWith("RR-");
        assertThat(receipt.getTotalFines()).isEqualByComparingTo(BigDecimal.ZERO);

        BookCopy copy = findCopyByBarcode("BC-EJ-01");
        assertThat(copy.getStatus()).isEqualTo(CopyStatus.AVAILABLE);

        Loan updatedLoan = loanRepository.findById(loan.getId()).orElseThrow();
        assertThat(updatedLoan.getStatus()).isEqualTo(LoanStatus.RETURNED);
    }

    @Test
    @DisplayName("Tasks 4.11 & 4.13: Receive return damaged/lost incurs fee and synchronizes fine and copy status")
    void test4_11_and_4_13_ReceiveReturnDamagedOrLostWithFines() {
        Loan loan = loanRepository.save(Loan.builder()
                .copy(copy1).member(member).dueDate(LocalDate.now().plusDays(5)).status(LoanStatus.BORROWING).build());
        copy1.setStatus(CopyStatus.BORROWED);
        bookCopyRepository.save(copy1);
        loanItemRepository.save(LoanItem.builder()
                .loan(loan).copy(copy1).status(LoanItemStatus.BORROWING).dueDate(LocalDate.now().plusDays(5)).build());

        ReturnReceiptViewDto receipt = loanService.receiveReturn("BC-EJ-01", ReturnCondition.DAMAGED, "Coffee stain on page 20-30", librarian.getEmail());
        assertThat(receipt.getTotalFines()).isGreaterThan(BigDecimal.ZERO);

        BookCopy copy = findCopyByBarcode("BC-EJ-01");
        assertThat(copy.getStatus()).isEqualTo(CopyStatus.DAMAGED);

        List<Fine> fines = fineRepository.findByMember_IdAndStatus(member.getId(), FineStatus.UNPAID);
        assertThat(fines).isNotEmpty();
    }

    @Test
    @DisplayName("Task 4.12: Return receipt view retrieval and formatted details")
    void test4_12_ReturnReceiptGenerationAndRetrieval() {
        Loan loan = loanRepository.save(Loan.builder()
                .copy(copy2).member(member).dueDate(LocalDate.now().plusDays(5)).status(LoanStatus.BORROWING).build());
        copy2.setStatus(CopyStatus.BORROWED);
        bookCopyRepository.save(copy2);
        loanItemRepository.save(LoanItem.builder()
                .loan(loan).copy(copy2).status(LoanItemStatus.BORROWING).dueDate(LocalDate.now().plusDays(5)).build());

        ReturnReceiptViewDto created = loanService.receiveReturn("BC-EJ-02", ReturnCondition.GOOD, "Excellent condition", librarian.getEmail());
        ReturnReceiptViewDto retrieved = loanService.getReturnReceipt(created.getReceiptCode());

        assertThat(retrieved).isNotNull();
        assertThat(retrieved.getReceiptCode()).isEqualTo(created.getReceiptCode());
        assertThat(retrieved.getMemberName()).isEqualTo("Nguyễn Minh Anh");
        assertThat(retrieved.getItems()).hasSize(1);
    }

    @Test
    @DisplayName("Task 4.13: Returning a book with pending reservations marks reservation READY and copy RESERVED")
    void test4_13_ReceiveReturnSynchronizesReservationQueue() {
        User resvMember = userRepository.save(User.builder()
                .fullName("Lê Văn Chờ").email("waiting@booknet.local").password("Password123")
                .role(UserRole.ROLE_USER).status(UserStatus.ACTIVE).provider(AuthProvider.LOCAL).emailVerified(true).build());

        Reservation resv = reservationRepository.save(Reservation.builder()
                .book(book).member(resvMember).status(ReservationStatus.PENDING).build());

        Loan loan = loanRepository.save(Loan.builder()
                .copy(copy1).member(member).dueDate(LocalDate.now().plusDays(5)).status(LoanStatus.BORROWING).build());
        copy1.setStatus(CopyStatus.BORROWED);
        bookCopyRepository.save(copy1);
        loanItemRepository.save(LoanItem.builder()
                .loan(loan).copy(copy1).status(LoanItemStatus.BORROWING).dueDate(LocalDate.now().plusDays(5)).build());

        loanService.receiveReturn("BC-EJ-01", ReturnCondition.GOOD, "Returned on time", librarian.getEmail());

        Reservation updatedResv = reservationRepository.findById(resv.getId()).orElseThrow();
        assertThat(updatedResv.getStatus()).isEqualTo(ReservationStatus.READY);

        BookCopy updatedCopy = findCopyByBarcode("BC-EJ-01");
        assertThat(updatedCopy.getStatus()).isEqualTo(CopyStatus.RESERVED);
    }

    @Test
    @DisplayName("Task 4.14: Renew loan item: extends 7 days, limit 1 renewal, blocked if overdue or reserved")
    void test4_14_RenewLoanItemSuccessAndRestrictions() {
        LocalDate initialDueDate = LocalDate.now().plusDays(5);
        Loan loan = loanRepository.save(Loan.builder()
                .copy(copy1).member(member).dueDate(initialDueDate).status(LoanStatus.BORROWING).build());
        LoanItem item = loanItemRepository.save(LoanItem.builder()
                .loan(loan).copy(copy1).status(LoanItemStatus.BORROWING).dueDate(initialDueDate).renewCount(0).build());

        // 1. Success first renewal
        LoanItem renewed = loanService.renewLoanItem(item.getId(), librarian.getEmail());
        assertThat(renewed.getRenewCount()).isEqualTo(1);
        assertThat(renewed.getDueDate()).isEqualTo(initialDueDate.plusDays(7));

        // 2. Fails second renewal (max 1)
        assertThatThrownBy(() -> loanService.renewLoanItem(item.getId(), librarian.getEmail()))
                .isInstanceOf(LoanException.class)
                .hasMessageContaining("limit reached");
    }

    @Test
    @DisplayName("MVC UI: /librarian/loans and /admin/loans render dark sidebar, account topbar and stats tables")
    @WithMockUser(username = "librarian.flow@booknet.local", authorities = {"ROLE_ADMIN", "CIRCULATION"})
    void testMvc_LibrarianDeskPagesRenderWithSecurityAndSidebar() throws Exception {
        mvc.perform(get("/librarian/loans"))
                .andExpect(status().isOk())
                .andExpect(view().name("librarian/loans"))
                .andExpect(model().attributeExists("summary", "currentTab"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("admin-sidebar")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("admin-topbar")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Loans &amp; Returns Desk")));

        mvc.perform(get("/librarian/loans/receive-return"))
                .andExpect(status().isOk())
                .andExpect(view().name("librarian/receive-return"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Receive Book Return at Desk")));
    }

    @Test
    @DisplayName("Librarian circulation desk accessible via /staff/loans")
    @WithMockUser(username = "librarian@booknet.local", authorities = {"ROLE_LIBRARIAN", "CIRCULATION"})
    void testLibrarianAccessCirculationDesk() throws Exception {
        mvc.perform(get("/staff/loans"))
                .andExpect(status().isOk())
                .andExpect(view().name("librarian/loans"));
    }

    @Test
    void circulationAliasesRejectUsersWithoutCirculationPermission() throws Exception {
        for (String role : List.of("ROLE_MEMBER", "ROLE_LIBRARIAN", "ROLE_ADMIN")) {
            for (String base : List.of("/staff/loans", "/librarian/loans", "/admin/loans")) {
                var principal = org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("restricted")
                        .authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority(role),
                                new org.springframework.security.core.authority.SimpleGrantedAuthority("MEMBER_READ"));
                mvc.perform(get(base).with(principal)).andExpect(status().isForbidden());
                mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(base + "/requests/1/approve")
                        .with(principal).with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()))
                        .andExpect(status().isForbidden());
            }
        }
    }

    @Test
    void expiredCardCannotBeApprovedOrHandedOver() {
        member.getMemberCard().setExpiresOn(LocalDate.now().minusDays(1));
        userRepository.saveAndFlush(member);
        assertThat(loanService.lookupMember(member.getEmail()).isEligible()).isFalse();
        var request = borrowRequestRepository.saveAndFlush(BorrowRequest.builder()
                .requestCode("BR-EXPIRED").member(member).status(BorrowRequestStatus.PENDING).build());
        assertThatThrownBy(() -> loanService.approveBorrowRequest(request.getId(), librarian.getEmail()))
                .isInstanceOf(grp3.java.booknet.exception.borrowing.LoanException.class);
        request.setStatus(BorrowRequestStatus.APPROVED);borrowRequestRepository.saveAndFlush(request);
        assertThatThrownBy(() -> loanService.handoverLoan(request.getId(), List.of(copy1.getBarcode()), null, librarian.getEmail()))
                .isInstanceOf(grp3.java.booknet.exception.borrowing.LoanException.class);
        assertThat(copy1.getStatus()).isEqualTo(CopyStatus.AVAILABLE);
    }

    @Test
    void activeAccountStatusAloneDoesNotMakeMemberEligible() {
        assertThat(loanService.lookupMember(librarian.getEmail())).isNull();
        member.setEnabled(false);userRepository.saveAndFlush(member);
        assertThat(loanService.lookupMember(member.getEmail()).isEligible()).isFalse();
        member.setEnabled(true);member.setEmailVerified(false);userRepository.saveAndFlush(member);
        assertThat(loanService.lookupMember(member.getEmail()).isEligible()).isFalse();
    }

    @Test
    void circulationTabsAndSharedNavigationRender() throws Exception {
        var output = java.nio.file.Path.of("target/post-pull-ui");java.nio.file.Files.createDirectories(output);
        String assets = java.nio.file.Path.of("src/main/resources/static").toAbsolutePath().toUri().toString();
        for (String tab : List.of("pending_approval", "awaiting_handover", "active_loans", "pending_returns", "history")) {
            String html = mvc.perform(get("/staff/loans").param("tab",tab)).andExpect(status().isOk())
                    .andExpect(content().string(org.hamcrest.Matchers.containsString("admin-sidebar")))
                    .andReturn().getResponse().getContentAsString();
            java.nio.file.Files.writeString(output.resolve(tab+".html"),html.replace("href=\"/css/","href=\""+assets+"css/")
                    .replace("src=\"/images/","src=\""+assets+"images/").replace("src=\"/js/","src=\""+assets+"js/"));
        }
    }
}
