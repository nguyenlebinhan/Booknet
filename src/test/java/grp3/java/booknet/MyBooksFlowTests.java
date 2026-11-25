package grp3.java.booknet;

import grp3.java.booknet.entity.book.Author;
import grp3.java.booknet.entity.book.Book;
import grp3.java.booknet.entity.book.BookCopy;
import grp3.java.booknet.entity.book_service.Loan;
import grp3.java.booknet.entity.user.User;
import grp3.java.booknet.enums.book.CopyStatus;
import grp3.java.booknet.enums.book_service.LoanStatus;
import grp3.java.booknet.enums.user.AuthProvider;
import grp3.java.booknet.enums.user.UserRole;
import grp3.java.booknet.enums.user.UserStatus;
import grp3.java.booknet.repository.book.AuthorRepository;
import grp3.java.booknet.repository.book.BookCopyRepository;
import grp3.java.booknet.repository.book.BookRepository;
import grp3.java.booknet.repository.book_service.LoanRepository;
import grp3.java.booknet.repository.book_service.ReservationRepository;
import grp3.java.booknet.repository.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MyBooksFlowTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private BookCopyRepository bookCopyRepository;

    @Autowired
    private LoanRepository loanRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private AuthorRepository authorRepository;

    @Autowired
    private grp3.java.booknet.repository.book_service.FavoriteBookRepository favoriteBookRepository;

    @Autowired
    private grp3.java.booknet.repository.book_service.BorrowRequestRepository borrowRequestRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private grp3.java.booknet.repository.user.RoleDefinitionRepository roles;

    private static final String TEST_PASSWORD = TestPasswords.valid();

    private User member;
    private Loan activeLoan;
    private Loan overdueLoan;

    @BeforeEach
    void setUp() {
        favoriteBookRepository.deleteAll();
        borrowRequestRepository.deleteAll();
        reservationRepository.deleteAll();
        loanRepository.deleteAll();
        bookCopyRepository.deleteAll();
        bookRepository.deleteAll();
        authorRepository.deleteAll();
        userRepository.deleteAll();

        member = userRepository.saveAndFlush(User.builder()
                .fullName("Nguyễn Minh Anh")
                .email("minhanh@booknet.test")
                .username("minhanh")
                .password(passwordEncoder.encode(TEST_PASSWORD))
                .memberCode("TV-02481")
                .role(UserRole.ROLE_USER)
                .provider(AuthProvider.LOCAL)
                .enabled(true)
                .emailVerified(true)
                .status(UserStatus.ACTIVE)
                .build());

        Author author = authorRepository.saveAndFlush(Author.builder().name("Tô Hoài").build());

        Book book1 = bookRepository.saveAndFlush(Book.builder()
                .title("Dế Mèn phiêu lưu ký")
                .isbn("978-604-1-11111-1")
                .authors(Set.of(author))
                .build());

        BookCopy copy1 = bookCopyRepository.saveAndFlush(BookCopy.builder()
                .book(book1)
                .barcode("LM-2026-0258")
                .status(CopyStatus.BORROWED)
                .build());

        // Loan còn 3 ngày nữa đến hạn
        activeLoan = loanRepository.saveAndFlush(Loan.builder()
                .member(member)
                .copy(copy1)
                .borrowedAt(LocalDateTime.now().minusDays(11))
                .dueDate(LocalDate.now().plusDays(3))
                .status(LoanStatus.BORROWING)
                .renewCount(0)
                .build());

        Book book2 = bookRepository.saveAndFlush(Book.builder()
                .title("Sapiens")
                .isbn("978-604-1-22222-2")
                .authors(Set.of(author))
                .build());

        BookCopy copy2 = bookCopyRepository.saveAndFlush(BookCopy.builder()
                .book(book2)
                .barcode("LM-2026-0241")
                .status(CopyStatus.BORROWED)
                .build());

        // Loan quá hạn 2 ngày
        overdueLoan = loanRepository.saveAndFlush(Loan.builder()
                .member(member)
                .copy(copy2)
                .borrowedAt(LocalDateTime.now().minusDays(16))
                .dueDate(LocalDate.now().minusDays(2))
                .status(LoanStatus.OVERDUE)
                .renewCount(0)
                .build());
    }

    @Test
    @DisplayName("Anonymous users cannot access My books and are redirected to login")
    void anonymousCannotAccessMyBooks() throws Exception {
        mockMvc.perform(get("/member/my-books"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    @DisplayName("Member can view current loans, overdue alert and due date calculations")
    void memberCanViewLoansAndHistory() throws Exception {
        MockHttpSession session = loginMember();
        mockMvc.perform(get("/member/my-books").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("borrowing/my-books"))
                .andExpect(model().attributeExists("view"))
                .andExpect(content().string(containsString("The books keeping you company.")))
                .andExpect(content().string(containsString("Dế Mèn phiêu lưu ký")))
                .andExpect(content().string(containsString("Sapiens")))
                .andExpect(content().string(containsString("2 days overdue")))
                .andExpect(content().string(containsString("Attention: Overdue Notice")));
    }

    @Test
    @DisplayName("Member can request return for active loan")
    void memberCanRequestReturn() throws Exception {
        MockHttpSession session = loginMember();
        mockMvc.perform(post("/member/loans/" + activeLoan.getId() + "/return")
                        .session(session)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/member/my-books"))
                .andExpect(flash().attributeExists("successMessage"));

        Loan updated = loanRepository.findById(activeLoan.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(LoanStatus.RETURN_PENDING);
        assertThat(updated.getReturnRequestedAt()).isNotNull();
    }

    @Test
    @DisplayName("Member can request 7-day renewal for eligible loan")
    void memberCanRequestRenew() throws Exception {
        MockHttpSession session = loginMember();
        mockMvc.perform(post("/member/loans/" + activeLoan.getId() + "/renew")
                        .session(session)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/member/my-books"))
                .andExpect(flash().attributeExists("successMessage"));

        Loan updated = loanRepository.findById(activeLoan.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(LoanStatus.RENEW_PENDING);
        assertThat(updated.getRenewRequestedAt()).isNotNull();
    }

    @Test
    @DisplayName("Member cannot renew overdue book")
    void memberCannotRenewOverdueBook() throws Exception {
        MockHttpSession session = loginMember();
        mockMvc.perform(post("/member/loans/" + overdueLoan.getId() + "/renew")
                        .session(session)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/member/my-books"))
                .andExpect(flash().attributeExists("errorMessage"));

        Loan updated = loanRepository.findById(overdueLoan.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(LoanStatus.OVERDUE);
    }

    @Test
    void disabledRenewPermissionHidesActionsAndRejectsSubmission() throws Exception {
        var role = roles.findBySystemRole(UserRole.ROLE_USER).orElseThrow();
        var original = new java.util.HashSet<>(role.getCapabilities());
        try {
            role.getCapabilities().remove(grp3.java.booknet.enums.user.RoleCapability.RENEW);
            roles.saveAndFlush(role);
            MockHttpSession session = loginMember();
            String html = mockMvc.perform(get("/member/my-books").session(session))
                    .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
            assertThat(html).doesNotContain("action=\"/member/loans/" + activeLoan.getId() + "/renew\"");
            mockMvc.perform(post("/member/loans/" + activeLoan.getId() + "/renew").session(session).with(csrf()))
                    .andExpect(status().isForbidden());
            assertThat(loanRepository.findById(activeLoan.getId()).orElseThrow().getStatus()).isEqualTo(LoanStatus.BORROWING);
        } finally {
            var current = roles.findById(role.getId()).orElseThrow();
            current.setCapabilities(original);roles.saveAndFlush(current);
        }
    }

    private MockHttpSession loginMember() throws Exception {
        return (MockHttpSession) mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("email", "minhanh@booknet.test")
                        .param("password", TEST_PASSWORD))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"))
                .andReturn().getRequest().getSession();
    }
}
