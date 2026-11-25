package grp3.java.booknet;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mail.javamail.JavaMailSender;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import grp3.java.booknet.dto.member.MemberDirectoryView;
import grp3.java.booknet.entity.book.Book;
import grp3.java.booknet.entity.book.BookCopy;
import grp3.java.booknet.entity.book_service.Fine;
import grp3.java.booknet.entity.book_service.Loan;
import grp3.java.booknet.entity.book_service.LoanItem;
import grp3.java.booknet.entity.user.MemberCard;
import grp3.java.booknet.entity.user.User;
import grp3.java.booknet.enums.book_service.FineReason;
import grp3.java.booknet.enums.book_service.FineStatus;
import grp3.java.booknet.enums.book_service.LoanItemStatus;
import grp3.java.booknet.enums.book_service.LoanStatus;
import grp3.java.booknet.enums.user.AuthProvider;
import grp3.java.booknet.enums.user.UserRole;
import grp3.java.booknet.enums.user.UserStatus;
import grp3.java.booknet.repository.book.BookCopyRepository;
import grp3.java.booknet.repository.book.BookRepository;
import grp3.java.booknet.repository.book_service.FineRepository;
import grp3.java.booknet.repository.book_service.LoanItemRepository;
import grp3.java.booknet.repository.book_service.LoanRepository;
import grp3.java.booknet.repository.user.AccountAccessAuditRepository;
import grp3.java.booknet.repository.user.AccountTokenRepository;
import grp3.java.booknet.repository.user.OAuthIdentityRepository;
import grp3.java.booknet.repository.user.UserRepository;
import grp3.java.booknet.security.dto.CustomUserDetails;
import grp3.java.booknet.service.fine.FineService;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:member_directory_live;MODE=MySQL;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc @ActiveProfiles("test")
class MemberDirectoryLiveDataTests {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired LoanRepository loans;
    @Autowired LoanItemRepository items;
    @Autowired FineRepository fines;
    @Autowired BookRepository books;
    @Autowired BookCopyRepository copies;
    @Autowired AccountTokenRepository tokens;
    @Autowired OAuthIdentityRepository identities;
    @Autowired AccountAccessAuditRepository audits;
    @Autowired FineService fineService;
    @Autowired Clock clock;
    @MockitoBean JavaMailSender mail;
    User librarian, admin, member;
    int sequence;

    @BeforeEach void setup() {
        clean();
        sequence = 0;
        librarian = userAccount("Library Staff", UserRole.ROLE_LIBRARIAN);
        admin = userAccount("Library Admin", UserRole.ROLE_ADMIN);
        member = userAccount("Actual Reader", UserRole.ROLE_USER);
    }
    @AfterEach void clean() {
        tokens.deleteAll(); identities.deleteAll(); audits.deleteAll();
        fines.deleteAll(); items.deleteAll(); loans.deleteAll(); copies.deleteAll(); books.deleteAll(); users.deleteAll();
    }
    User userAccount(String name, UserRole role) {
        return users.saveAndFlush(User.builder().fullName(name).email("live" + (++sequence) + "@booknet.test")
                .role(role).provider(AuthProvider.LOCAL).enabled(true).emailVerified(true).status(UserStatus.ACTIVE).build());
    }
    LocalDate today() { return LocalDate.now(clock); }
    BookCopy copy(String title) {
        var book = books.saveAndFlush(Book.builder().title(title).bookCode("LIVE-" + (++sequence)).build());
        return copies.saveAndFlush(BookCopy.builder().book(book).barcode("LIVE-COPY-" + sequence).build());
    }
    Loan loan(User who, String title, LocalDate due, LoanStatus status) {
        return loans.saveAndFlush(Loan.builder().member(who).copy(copy(title)).dueDate(due).status(status).build());
    }
    LoanItem item(Loan loan, BookCopy copy, LocalDate due, LoanItemStatus status) {
        return items.saveAndFlush(LoanItem.builder().loan(loan).copy(copy).dueDate(due).status(status)
                .returnedAt(status == LoanItemStatus.RETURNED ? LocalDateTime.now(clock) : null).build());
    }
    Fine fine(User who, Loan loan, String amount, FineStatus status) {
        return fines.saveAndFlush(Fine.builder().member(who).loan(loan).amount(new BigDecimal(amount))
                .reason(FineReason.OVERDUE).fineCode("LIVE-FINE-" + (++sequence)).status(status).build());
    }
    MvcResult directory(String... pairs) throws Exception {
        var request = get("/staff/members").with(user(new CustomUserDetails(librarian)));
        for (int i = 0; i < pairs.length; i += 2) request.param(pairs[i], pairs[i + 1]);
        return mvc.perform(request).andExpect(status().isOk()).andReturn();
    }
    MemberDirectoryView.Directory data(MvcResult result) {
        return (MemberDirectoryView.Directory) result.getModelAndView().getModel().get("directory");
    }
    MvcResult detail(User actor, String base, User who) throws Exception {
        return mvc.perform(get(base + "/" + who.getId()).with(user(new CustomUserDetails(actor))))
                .andExpect(status().isOk()).andReturn();
    }
    MemberDirectoryView.Person person(MvcResult result) {
        return (MemberDirectoryView.Person) result.getModelAndView().getModel().get("member");
    }

    @Test void filtersUseRealBalancesBeforePaginationAndKeepParameters() throws Exception {
        for (int i = 0; i < 8; i++) {
            var who = userAccount("Filter Reader " + i, UserRole.ROLE_USER);
            var card = new MemberCard(); card.setUser(who); card.setCardNumber("FILTER-CARD-" + i);
            card.setIssuedOn(today().minusDays(10)); card.setExpiresOn(today().plusDays(30));
            who.setMemberCard(card); users.saveAndFlush(who);
            var loan = loan(who, "Filter Book " + i, today().minusDays(2), LoanStatus.BORROWING);
            fine(who, loan, "10000", FineStatus.UNPAID);
        }
        var first = directory("keyword", "Filter Reader", "loan", "overdue", "fine", "unpaid", "card", "Active");
        assertThat(data(first).total()).isEqualTo(8);
        assertThat(data(first).pages()).isEqualTo(2);
        assertThat(data(first).people()).hasSize(7).allSatisfy(p -> {
            assertThat(p.onLoan()).isEqualTo(1); assertThat(p.overdue()).isEqualTo(1); assertThat(p.outstanding()).isEqualTo("10.000\u0111");
        });
        assertThat(first.getResponse().getContentAsString()).contains("loan=overdue", "fine=unpaid").doesNotContain("Loan and fine filters are available in sample preview");
        var last = directory("keyword", "Filter Reader", "loan", "overdue", "fine", "unpaid", "page", "99");
        assertThat(data(last).page()).isEqualTo(1); assertThat(data(last).people()).hasSize(1);
        assertThat(data(directory("keyword", "Filter Reader", "loan", "none")).total()).isZero();
        assertThat(data(directory("keyword", "filter-card-3")).total()).isEqualTo(1);
    }

    @Test void childCopiesAreAuthoritativeAndLegacyRenewalsRemainVisible() throws Exception {
        var parent = loan(member, "Child overdue book", today().minusDays(3), LoanStatus.BORROWING);
        item(parent, parent.getCopy(), today().minusDays(3), LoanItemStatus.BORROWED);
        item(parent, copy("Due today return requested"), today(), LoanItemStatus.RETURN_REQUESTED);
        item(parent, copy("Already returned copy"), today().minusDays(5), LoanItemStatus.RETURNED);
        loan(member, "Legacy pending renewal", today().minusDays(1), LoanStatus.RENEW_PENDING);
        var closedParent = loan(member, "Closed child despite old parent", today().minusDays(5), LoanStatus.BORROWING);
        item(closedParent, closedParent.getCopy(), today().minusDays(5), LoanItemStatus.RETURNED);
        loan(userAccount("Another reader", UserRole.ROLE_USER), "Someone else's book", today().minusDays(4), LoanStatus.OVERDUE);
        var result = detail(librarian, "/staff/members", member);
        assertThat(person(result).onLoan()).isEqualTo(3); assertThat(person(result).overdue()).isEqualTo(2);
        assertThat(data(directory("keyword", "Actual Reader", "loan", "borrowed")).people()).singleElement()
                .satisfies(p -> { assertThat(p.onLoan()).isEqualTo(3); assertThat(p.overdue()).isEqualTo(2); });
        String html = result.getResponse().getContentAsString();
        assertThat(html).contains("Child overdue book", "Legacy pending renewal", "awaiting physical receipt")
                .doesNotContain("Already returned copy", "Someone else&#39;s book", "Closed child despite old parent", "not connected yet");
        assertThat(loans.findById(parent.getId()).orElseThrow().getStatus()).isEqualTo(LoanStatus.BORROWING);
        write("live-details", html);
        write("live-directory", directory().getResponse().getContentAsString());
        write("live-admin-details", detail(admin, "/admin/users", member).getResponse().getContentAsString());
    }

    @Test void onlyFinalizedUnpaidFinesAffectBalancesAndPaymentUpdatesTheDirectory() throws Exception {
        var returned = loan(member, "Returned fine book", today().minusDays(5), LoanStatus.RETURNED);
        var unpaid = fine(member, returned, "10000", FineStatus.UNPAID);
        fine(member, returned, "20000", FineStatus.PAID);
        fine(member, returned, "30000", FineStatus.WAIVED);
        loan(member, "Overdue estimate from fines service", today().minusDays(2), LoanStatus.OVERDUE);
        var result = detail(librarian, "/staff/members", member);
        assertThat(person(result).outstanding()).isEqualTo("10.000\u0111");
        assertThat(result.getModelAndView().getModel().get("estimatedFine")).isNotEqualTo("0\u0111");
        assertThat(result.getResponse().getContentAsString()).contains(
                result.getModelAndView().getModel().get("estimatedFine").toString());
        assertThat(result.getResponse().getContentAsString()).contains("Finalized", "Unpaid", "Paid", "Waived", "Estimated");
        assertThat(data(directory("fine", "unpaid")).people()).extracting(MemberDirectoryView.Person::id).containsExactly(member.getId());
        fineService.confirmPaymentAtDesk(unpaid.getId(), librarian, "TEST-LIVE-PAYMENT");
        assertThat(data(directory("fine", "unpaid")).total()).isZero();
        assertThat(data(directory("keyword", "Actual Reader", "fine", "clear")).total()).isEqualTo(1);
        assertThat(person(detail(admin, "/admin/users", member)).outstanding()).isEqualTo("0\u0111");
        assertThat(users.findById(member.getId()).orElseThrow().isEnabled()).isTrue();
    }

    @Test void liveDataKeepsStaffAndMemberAuthorizationBoundaries() throws Exception {
        var staffLoan = loan(admin, "Administrative borrower", today().minusDays(1), LoanStatus.BORROWING);
        fine(admin, staffLoan, "5000", FineStatus.UNPAID);
        assertThat(data(directory("loan", "borrowed", "fine", "unpaid")).total()).isZero();
        mvc.perform(get("/staff/members/" + admin.getId()).with(user(new CustomUserDetails(librarian)))).andExpect(status().isNotFound());
        mvc.perform(get("/staff/members/" + member.getId()).with(user(new CustomUserDetails(member)))).andExpect(status().isForbidden());
        mvc.perform(get("/staff/members").param("fine", "unpaid")).andExpect(status().is3xxRedirection());
        var result = detail(admin, "/admin/users", admin);
        assertThat(person(result).outstanding()).isEqualTo("5.000\u0111");
    }

    @Test void emptyHistoryShowsZeroAndNeverInventsLoansOrFines() throws Exception {
        var result = detail(librarian, "/staff/members", member);
        assertThat(person(result).onLoan()).isZero(); assertThat(person(result).overdue()).isZero();
        assertThat(person(result).outstanding()).isEqualTo("0\u0111");
        assertThat(result.getResponse().getContentAsString()).contains("No current loans", "No recorded or estimated fines")
                .doesNotContain("Not connected", "Loan information is not connected yet");
        assertThat(data(directory("loan", "none", "fine", "clear")).total()).isEqualTo(1);
    }

    private void write(String name, String html) throws Exception {
        Path output = Path.of("target/member-live-review"); Files.createDirectories(output);
        String assets = Path.of("src/main/resources/static").toAbsolutePath().toUri().toString();
        Files.writeString(output.resolve(name + ".html"), html.replace("href=\"/css/", "href=\"" + assets + "css/")
                .replace("src=\"/images/", "src=\"" + assets + "images/").replace("src=\"/js/", "src=\"" + assets + "js/"));
    }
}
