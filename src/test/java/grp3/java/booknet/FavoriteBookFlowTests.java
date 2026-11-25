package grp3.java.booknet;

import grp3.java.booknet.entity.book.Author;
import grp3.java.booknet.entity.book.Book;
import grp3.java.booknet.entity.book.BookCopy;
import grp3.java.booknet.entity.user.MemberCard;
import grp3.java.booknet.entity.user.User;
import grp3.java.booknet.enums.book.CopyStatus;
import grp3.java.booknet.enums.book.PublicationStatus;
import grp3.java.booknet.enums.user.AuthProvider;
import grp3.java.booknet.enums.user.UserRole;
import grp3.java.booknet.enums.user.UserStatus;
import grp3.java.booknet.repository.book.AuthorRepository;
import grp3.java.booknet.repository.book.BookCopyRepository;
import grp3.java.booknet.repository.book.BookRepository;
import grp3.java.booknet.repository.book_service.BorrowRequestRepository;
import grp3.java.booknet.repository.book_service.FavoriteBookRepository;
import grp3.java.booknet.repository.book_service.LoanRepository;
import grp3.java.booknet.repository.book_service.ReservationRepository;
import grp3.java.booknet.repository.user.MemberCardRepository;
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
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FavoriteBookFlowTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MemberCardRepository memberCardRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private BookCopyRepository bookCopyRepository;

    @Autowired
    private AuthorRepository authorRepository;

    @Autowired
    private FavoriteBookRepository favoriteBookRepository;

    @Autowired
    private BorrowRequestRepository borrowRequestRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private LoanRepository loanRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private static final String TEST_PASSWORD = "Aa1TestPassword!";
    private User member;
    private Book book;

    @BeforeEach
    void setUp() {
        favoriteBookRepository.deleteAll();
        borrowRequestRepository.deleteAll();
        reservationRepository.deleteAll();
        loanRepository.deleteAll();
        bookCopyRepository.deleteAll();
        bookRepository.deleteAll();
        authorRepository.deleteAll();
        memberCardRepository.deleteAll();
        userRepository.deleteAll();

        member = userRepository.saveAndFlush(User.builder()
                .fullName("Favorite Tester")
                .email("favtester@booknet.test")
                .username("favtester")
                .password(passwordEncoder.encode(TEST_PASSWORD))
                .memberCode("TV-99999")
                .role(UserRole.ROLE_USER)
                .provider(AuthProvider.LOCAL)
                .enabled(true)
                .emailVerified(true)
                .status(UserStatus.ACTIVE)
                .build());

        MemberCard card = new MemberCard();
        card.setUser(member);
        card.setCardNumber("BN-TEST-FAV");
        card.setIssuedOn(LocalDate.now().minusDays(1));
        card.setExpiresOn(LocalDate.now().plusYears(1));
        memberCardRepository.saveAndFlush(card);

        Author author = authorRepository.saveAndFlush(Author.builder()
                .name("Haruki Murakami")
                .build());

        book = bookRepository.saveAndFlush(Book.builder()
                .title("Norwegian Wood")
                .isbn("978-0-099-44882-2")
                .publicationStatus(PublicationStatus.PUBLISHED)
                .authors(Set.of(author))
                .build());

        bookCopyRepository.saveAndFlush(BookCopy.builder()
                .book(book)
                .barcode("NW-001")
                .shelfLocation("Floor 2")
                .status(CopyStatus.AVAILABLE)
                .build());
    }

    private MockHttpSession loginMember() throws Exception {
        return (MockHttpSession) mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("email", "favtester@booknet.test")
                        .param("password", TEST_PASSWORD))
                .andExpect(status().is3xxRedirection())
                .andReturn().getRequest().getSession();
    }

    @Test
    @DisplayName("Member can toggle favorite book from catalog/details page")
    void memberCanToggleFavorite() throws Exception {
        MockHttpSession session = loginMember();

        // 1. Toggle add
        mockMvc.perform(post("/member/favorites/" + book.getId() + "/toggle")
                        .session(session)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/catalog/" + book.getId()))
                .andExpect(flash().attributeExists("successMessage"));

        assertThat(favoriteBookRepository.existsByUser_IdAndBook_Id(member.getId(), book.getId())).isTrue();

        // 2. View details page shows book as favorite
        mockMvc.perform(get("/catalog/" + book.getId())
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(model().attribute("isFavorite", true));

        // 3. View my-books with favorites tab
        mockMvc.perform(get("/member/my-books")
                        .session(session)
                        .param("tab", "favorites"))
                .andExpect(status().isOk())
                .andExpect(view().name("borrowing/my-books"))
                .andExpect(model().attributeExists("view"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Norwegian Wood")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Request loan")));

        // 4. Toggle remove
        mockMvc.perform(post("/member/favorites/" + book.getId() + "/toggle")
                        .session(session)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection());

        assertThat(favoriteBookRepository.existsByUser_IdAndBook_Id(member.getId(), book.getId())).isFalse();
    }

    @Test
    @DisplayName("Member can remove favorite directly from my-books favorites tab")
    void memberCanRemoveFavoriteFromMyBooks() throws Exception {
        MockHttpSession session = loginMember();

        // Save book
        mockMvc.perform(post("/member/favorites/" + book.getId() + "/toggle")
                        .session(session)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection());

        assertThat(favoriteBookRepository.count()).isEqualTo(1);

        // Remove from my-books
        mockMvc.perform(post("/member/favorites/" + book.getId() + "/remove")
                        .session(session)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/member/my-books?tab=favorites"));

        assertThat(favoriteBookRepository.count()).isZero();
    }

    @Test
    @DisplayName("Request multiple saved books verifies loan limit constraint")
    void memberRequestMultipleSavedBooksRespectsLimit() throws Exception {
        MockHttpSession session = loginMember();

        // Create 5 additional books (total 6 books)
        for (int i = 1; i <= 5; i++) {
            Book extraBook = bookRepository.saveAndFlush(Book.builder()
                    .title("Extra Book " + i)
                    .isbn("978-0-000-0000" + i)
                    .publicationStatus(PublicationStatus.PUBLISHED)
                    .build());
            bookCopyRepository.saveAndFlush(BookCopy.builder()
                    .book(extraBook)
                    .barcode("EX-00" + i)
                    .shelfLocation("Floor 1")
                    .status(CopyStatus.AVAILABLE)
                    .build());
            // Add to favorites
            mockMvc.perform(post("/member/favorites/" + extraBook.getId() + "/toggle")
                            .session(session)
                            .with(csrf()))
                    .andExpect(status().is3xxRedirection());
        }

        // Add the initial book as favorite as well (total 6 favorites)
        mockMvc.perform(post("/member/favorites/" + book.getId() + "/toggle")
                        .session(session)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection());

        assertThat(favoriteBookRepository.count()).isEqualTo(6);

        // Access confirm loan with 2 books -> eligibleToBorrow = true
        var allFavorites = favoriteBookRepository.findAll();
        Long id1 = allFavorites.get(0).getBook().getId();
        Long id2 = allFavorites.get(1).getBook().getId();

        mockMvc.perform(get("/loans/confirm")
                        .session(session)
                        .param("bookId", String.valueOf(id1))
                        .param("bookId", String.valueOf(id2)))
                .andExpect(status().isOk())
                .andExpect(view().name("borrowing/confirm"))
                .andExpect(model().attribute("view", org.hamcrest.Matchers.hasToString(org.hamcrest.Matchers.containsString("eligibleToBorrow=true"))));

        // Access confirm loan with 6 books (exceeding MAX_LOAN_LIMIT of 5) -> eligibleToBorrow = false
        var builder = get("/loans/confirm").session(session);
        for (var fav : allFavorites) {
            builder.param("bookId", String.valueOf(fav.getBook().getId()));
        }

        mockMvc.perform(builder)
                .andExpect(status().isOk())
                .andExpect(view().name("borrowing/confirm"))
                .andExpect(model().attribute("view", org.hamcrest.Matchers.hasToString(org.hamcrest.Matchers.containsString("eligibleToBorrow=false"))))
                .andExpect(model().attribute("view", org.hamcrest.Matchers.hasToString(org.hamcrest.Matchers.containsString("You can only request 5 more book(s)"))));

        // Submit confirm loan for 2 saved books -> both should be removed from favorites
        mockMvc.perform(post("/loans/confirm")
                        .session(session)
                        .with(csrf())
                        .param("bookIds", String.valueOf(id1))
                        .param("bookIds", String.valueOf(id2))
                        .param("termsAccepted", "true"))
                .andExpect(status().is3xxRedirection());

        assertThat(favoriteBookRepository.existsByUser_IdAndBook_Id(member.getId(), id1)).isFalse();
        assertThat(favoriteBookRepository.existsByUser_IdAndBook_Id(member.getId(), id2)).isFalse();
        assertThat(favoriteBookRepository.count()).isEqualTo(4);
    }
}
