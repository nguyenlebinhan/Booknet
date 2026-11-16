package grp3.java.booknet;

import grp3.java.booknet.entity.book.Book;
import grp3.java.booknet.entity.book.BookCopy;
import grp3.java.booknet.entity.book_service.Loan;
import grp3.java.booknet.entity.book_service.Reservation;
import grp3.java.booknet.entity.user.User;
import grp3.java.booknet.enums.book.CopyStatus;
import grp3.java.booknet.enums.book.PublicationStatus;
import grp3.java.booknet.enums.book_service.LoanStatus;
import grp3.java.booknet.enums.book_service.ReservationStatus;
import grp3.java.booknet.enums.user.AuthProvider;
import grp3.java.booknet.enums.user.UserRole;
import grp3.java.booknet.enums.user.UserStatus;
import grp3.java.booknet.repository.book.BookCopyRepository;
import grp3.java.booknet.repository.book.BookRepository;
import grp3.java.booknet.repository.book_service.LoanRepository;
import grp3.java.booknet.repository.book_service.ReservationRepository;
import grp3.java.booknet.repository.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AdminDashboardControllerTests {

    private static final String ADMIN_EMAIL = "dashboard-admin@booknet.test";
    private static final String ADMIN_PASSWORD = "AdminPass1";

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
    private PasswordEncoder passwordEncoder;

    private User admin;

    @BeforeEach
    void createAdminAccount() {
        admin = userRepository.saveAndFlush(User.builder()
                .fullName("Dashboard Administrator")
                .email(ADMIN_EMAIL)
                .username("dashboard-admin")
                .password(passwordEncoder.encode(ADMIN_PASSWORD))
                .role(UserRole.ROLE_ADMIN)
                .provider(AuthProvider.LOCAL)
                .enabled(true)
                .emailVerified(true)
                .status(UserStatus.ACTIVE)
                .build());
    }

    @Test
    void adminLoginOpensDashboardWithLiveCountsAndSidebar() throws Exception {
        User member = userRepository.saveAndFlush(User.builder()
                .fullName("Dashboard Member")
                .email("dashboard-member@booknet.test")
                .username("dashboard-member")
                .password(passwordEncoder.encode("MemberPass1"))
                .role(UserRole.ROLE_USER)
                .provider(AuthProvider.LOCAL)
                .enabled(true)
                .emailVerified(true)
                .status(UserStatus.ACTIVE)
                .build());

        Book published = bookRepository.saveAndFlush(Book.builder()
                .title("Dashboard published title")
                .bookCode("DASH-001")
                .isbn("9780000001001")
                .publicationStatus(PublicationStatus.PUBLISHED)
                .build());
        bookRepository.saveAndFlush(Book.builder()
                .title("Dashboard unpublished title")
                .bookCode("DASH-002")
                .isbn("9780000001002")
                .publicationStatus(PublicationStatus.DRAFT)
                .build());

        BookCopy borrowedCopy = bookCopyRepository.saveAndFlush(BookCopy.builder()
                .book(published)
                .barcode("DASH-COPY-BORROWED")
                .status(CopyStatus.BORROWED)
                .build());
        bookCopyRepository.saveAndFlush(BookCopy.builder()
                .book(published)
                .barcode("DASH-COPY-AVAILABLE")
                .status(CopyStatus.AVAILABLE)
                .build());
        loanRepository.saveAndFlush(Loan.builder()
                .copy(borrowedCopy)
                .member(member)
                .dueDate(LocalDate.now().minusDays(1))
                .status(LoanStatus.BORROWING)
                .build());
        reservationRepository.saveAndFlush(Reservation.builder()
                .book(published)
                .member(member)
                .status(ReservationStatus.PENDING)
                .build());

        MvcResult login = mockMvc.perform(post("/login").with(csrf())
                        .param("email", ADMIN_EMAIL)
                        .param("password", ADMIN_PASSWORD))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/dashboard"))
                .andReturn();
        MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);

        mockMvc.perform(get("/admin/dashboard").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/dashboard"))
                .andExpect(content().string(containsString("Dashboard Administrator")))
                .andExpect(content().string(containsString("Published titles")))
                .andExpect(content().string(containsString(">1</strong>")))
                .andExpect(content().string(containsString("Registered members")))
                .andExpect(content().string(containsString("Copies currently on loan")))
                .andExpect(content().string(containsString("Overdue loans")))
                .andExpect(content().string(containsString("Unpublished titles")))
                .andExpect(content().string(containsString("Pending reservations")))
                .andExpect(content().string(containsString("Book catalog")))
                .andExpect(content().string(containsString("href=\"/staff/members\"")))
                .andExpect(content().string(containsString("href=\"/admin/users\"")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void signedInAdminVisitingLoginIsSentToDashboard() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/dashboard"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void memberCannotOpenAdminDashboard() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isForbidden());
    }
}
