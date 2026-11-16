package grp3.java.booknet;

import grp3.java.booknet.entity.book.Author;
import grp3.java.booknet.entity.book.Book;
import grp3.java.booknet.entity.book.BookCopy;
import grp3.java.booknet.entity.book_service.Reservation;
import grp3.java.booknet.entity.user.User;
import grp3.java.booknet.enums.book.CopyStatus;
import grp3.java.booknet.enums.book_service.ReservationStatus;
import grp3.java.booknet.enums.user.AuthProvider;
import grp3.java.booknet.enums.user.UserRole;
import grp3.java.booknet.enums.user.UserStatus;
import grp3.java.booknet.repository.book.AuthorRepository;
import grp3.java.booknet.repository.book.BookCopyRepository;
import grp3.java.booknet.repository.book.BookRepository;
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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BorrowFlowTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private BookCopyRepository bookCopyRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private AuthorRepository authorRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private static final String TEST_PASSWORD = TestPasswords.valid();

    private User member;
    private Book sampleBook;

    @BeforeEach
    void setUp() {
        reservationRepository.deleteAll();
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

        Author author = authorRepository.saveAndFlush(Author.builder()
                .name("Nguyễn Nhật Ánh")
                .build());

        sampleBook = bookRepository.saveAndFlush(Book.builder()
                .title("Tôi thấy hoa vàng trên cỏ xanh")
                .isbn("978-604-1-00001-1")
                .authors(Set.of(author))
                .build());

        bookCopyRepository.saveAndFlush(BookCopy.builder()
                .book(sampleBook)
                .barcode("VN001-06")
                .shelfLocation("Circulation desk · Floor 1")
                .status(CopyStatus.AVAILABLE)
                .build());
    }

    @Test
    @DisplayName("Anonymous users cannot access confirm loan page and are redirected to login")
    void anonymousCannotAccessBorrowConfirm() throws Exception {
        mockMvc.perform(get("/loans/confirm").param("bookId", String.valueOf(sampleBook.getId())))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    @DisplayName("Member with valid session can view confirm loan page with correct book details and policy")
    void memberCanViewConfirmLoanPage() throws Exception {
        MockHttpSession session = loginMember();
        mockMvc.perform(get("/loans/confirm")
                        .session(session)
                        .param("bookId", String.valueOf(sampleBook.getId())))
                .andExpect(status().isOk())
                .andExpect(view().name("borrow-confirm"))
                .andExpect(model().attributeExists("view", "borrowForm"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Tôi thấy hoa vàng trên cỏ xanh")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Nguyễn Minh Anh")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("TV-02481")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Copy VN001-06")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("14-day loan")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Up to 5 books at a time")));
    }

    @Test
    @DisplayName("Submitting loan request without agreeing to terms fails validation")
    void submitWithoutTermsAgreementFails() throws Exception {
        MockHttpSession session = loginMember();
        mockMvc.perform(post("/loans/confirm")
                        .session(session)
                        .with(csrf())
                        .param("bookIds", String.valueOf(sampleBook.getId()))
                        .param("termsAccepted", "false"))
                .andExpect(status().isOk())
                .andExpect(view().name("borrow-confirm"))
                .andExpect(model().hasErrors());

        assertThat(reservationRepository.count()).isZero();
    }

    @Test
    @DisplayName("Submitting loan request successfully creates pending reservation for librarians")
    void submitLoanRequestSucceeds() throws Exception {
        MockHttpSession session = loginMember();
        mockMvc.perform(post("/loans/confirm")
                        .session(session)
                        .with(csrf())
                        .param("bookIds", String.valueOf(sampleBook.getId()))
                        .param("termsAccepted", "true"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/?loan_requested=true"))
                .andExpect(flash().attributeExists("successMessage"));

        List<Reservation> reservations = reservationRepository.findAll();
        assertThat(reservations).hasSize(1);
        Reservation res = reservations.getFirst();
        assertThat(res.getMember().getId()).isEqualTo(member.getId());
        assertThat(res.getBook().getId()).isEqualTo(sampleBook.getId());
        assertThat(res.getStatus()).isEqualTo(ReservationStatus.PENDING);
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
