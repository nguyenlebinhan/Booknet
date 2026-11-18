package grp3.java.booknet;

import grp3.java.booknet.entity.book.*;
import grp3.java.booknet.entity.book_service.Loan;
import grp3.java.booknet.entity.user.User;
import grp3.java.booknet.enums.book.CopyStatus;
import grp3.java.booknet.enums.book.PublicationStatus;
import grp3.java.booknet.enums.book_service.LoanStatus;
import grp3.java.booknet.enums.user.AuthProvider;
import grp3.java.booknet.enums.user.UserRole;
import grp3.java.booknet.enums.user.UserStatus;
import grp3.java.booknet.repository.book.*;
import grp3.java.booknet.repository.book_service.LoanRepository;
import grp3.java.booknet.repository.user.UserRepository;
import grp3.java.booknet.security.dto.CustomUserDetails;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Acceptance checks for Person 2 (Public Library & Book Discovery), one test per point in the
 * task document (2.1–2.5) plus the shared navbar and footer. Real pages and forms on H2.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@TestMethodOrder(MethodOrderer.DisplayName.class)
class Person2AcceptanceTests {
    @Autowired MockMvc mvc;
    @Autowired BookRepository books;
    @Autowired BookCopyRepository copies;
    @Autowired AuthorRepository authors;
    @Autowired CategoryRepository categories;
    @Autowired PublisherRepository publishers;
    @Autowired LoanRepository loans;
    @Autowired UserRepository users;
    @MockitoBean JavaMailSender mail;

    Category novels;
    Category science;
    Author nhatAnh;
    Author harari;
    User member;

    @BeforeEach
    void prepare() {
        novels = categories.save(Category.builder().name("P2 test novels").build());
        science = categories.save(Category.builder().name("P2 test science").build());
        nhatAnh = authors.save(Author.builder().name("P2 Nguyễn Nhật Ánh").build());
        harari = authors.save(Author.builder().name("P2 Yuval Harari").build());
        member = users.save(User.builder().fullName("P2 Reader").email("p2-reader@example.com").role(UserRole.ROLE_USER)
                .provider(AuthProvider.LOCAL).status(UserStatus.ACTIVE).enabled(true).emailVerified(true).build());
    }

    // ------------------------------------------------------------------ 2.1 UC05

    @Test
    @DisplayName("2.1 UC05 Public portal: home, about and contact with collections, popular books, events, hours, policies and FAQs")
    void uc05() throws Exception {
        Book popular = book("P2 Popular novel", novels, nhatAnh, 2020, "vi", "9786040000001");
        book("P2 Second novel", novels, nhatAnh, 2018, "vi", "9786040000002");
        borrowedCopies(popular, 2);

        String home = page("/");
        assertThat(home).contains("Open a book,", "P2 test novels", "href=\"/catalog?categoryIds=" + novels.getId() + "\"",
                "On the shelves at booknet", "P2 Popular novel", "href=\"/catalog/" + popular.getId() + "\"",
                "What's on at booknet", "carefully selected titles", "fellow readers", "14 days")
                .containsPattern("booknet (welcomes you until|opens at) \\d{2}:\\d{2}");
        assertThat(page("/about")).contains("Where books bring like-minded readers together", "What's on at booknet");
        assertThat(page("/contact")).contains("Opening hours", "A little help, right here.", "faq");
    }

    // ------------------------------------------------------------------ 2.2 UC06

    @Test
    @DisplayName("2.2 UC06 Contact inquiry: validated form, sent over SMTP, result shown")
    void uc06() throws Exception {
        mvc.perform(post("/contact").with(csrf()).param("fullName", "P2 Reader").param("email", "p2-reader@example.com")
                        .param("subject", "Book donations").param("message", "I would like to donate some books."))
                .andExpect(redirectedUrl("/contact")).andExpect(flash().attributeExists("contactSuccess"));
        verify(mail).send(any(SimpleMailMessage.class));

        String invalid = mvc.perform(post("/contact").with(csrf()).param("fullName", "").param("email", "not-an-email")
                        .param("subject", "Book donations").param("message", ""))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(invalid).contains("Please enter your full name.", "Please enter a valid email address.", "Please enter your message.");
        mvc.perform(post("/contact").param("fullName", "P2 Reader").param("email", "p2-reader@example.com").param("message", "Hi"))
                .andExpect(status().isForbidden());

        doThrow(new MailSendException("down")).when(mail).send(any(SimpleMailMessage.class));
        mvc.perform(post("/contact").with(csrf()).param("fullName", "P2 Reader").param("email", "p2-reader@example.com")
                        .param("subject", "Other").param("message", "Is the library open on holidays?"))
                .andExpect(status().isOk()).andExpect(flash().attributeCount(0));
    }

    // ------------------------------------------------------------------ 2.3 UC07

    @Test
    @DisplayName("2.3 UC07 Catalog: search by title, author or ISBN; filter by genre, copies, language, year range; sort; pages; public books only")
    void uc07() throws Exception {
        Book shelf = book("P2 Shelf story", novels, nhatAnh, 2010, "vi", "9786040000011");
        copy(shelf, "P2-SHELF-1", CopyStatus.AVAILABLE);
        Book out = book("P2 Sapiens", science, harari, 2015, "en", "9786040000012");
        borrowedCopies(out, 1);
        books.save(Book.builder().title("P2 Hidden draft").publicationStatus(PublicationStatus.DRAFT).build());
        for (int i = 0; i < 6; i++) book("P2 Filler " + i, novels, nhatAnh, 2001 + i, "vi", "978604000002" + i);

        assertThat(catalog("keyword", "P2 Shelf")).contains("P2 Shelf story");
        assertThat(catalog("keyword", "P2 Yuval")).contains("P2 Sapiens").doesNotContain("P2 Shelf story");
        assertThat(catalog("keyword", "9786040000012")).contains("P2 Sapiens");
        assertThat(catalog("keyword", "P2", "categoryIds", science.getId().toString())).contains("P2 Sapiens").doesNotContain("P2 Shelf story");
        assertThat(catalog("keyword", "P2", "availability", "borrow")).contains("P2 Shelf story").doesNotContain("P2 Sapiens");
        assertThat(catalog("keyword", "P2", "availability", "reserve")).contains("P2 Sapiens").doesNotContain("P2 Shelf story");
        assertThat(catalog("keyword", "P2", "languages", "en")).contains("P2 Sapiens").doesNotContain("P2 Shelf story");
        assertThat(catalog("keyword", "P2", "yearFrom", "2009", "yearTo", "2016")).contains("P2 Shelf story", "P2 Sapiens")
                .doesNotContain("P2 Filler 0");
        String first = catalog("keyword", "P2");
        assertThat(first).contains("Showing <span>1</span>–<span>6</span>", "of <span>8</span> results", "Most popular").doesNotContain("P2 Hidden draft");
        assertThat(first.indexOf("P2 Sapiens")).isGreaterThan(-1);
        assertThat(catalog("keyword", "P2", "page", "1")).contains("Showing <span>7</span>–<span>8</span>", "of <span>8</span> results");
        // Title A–Z: the six "P2 Filler" books fill page 1 in order; Sapiens and Shelf story follow on page 2.
        String byTitle = catalog("keyword", "P2", "sort", "title-asc");
        assertThat(byTitle.indexOf("P2 Filler 0")).isLessThan(byTitle.indexOf("P2 Filler 5"));
        assertThat(byTitle).doesNotContain("P2 Sapiens");
        assertThat(catalog("keyword", "P2", "sort", "title-asc", "page", "1")).contains("P2 Sapiens", "P2 Shelf story");
    }

    // ------------------------------------------------------------------ 2.4 UC08

    @Test
    @DisplayName("2.4 UC08 Book details: full information, cover, floor/shelf, every copy's barcode and status, borrow or reserve")
    void uc08() throws Exception {
        Publisher publisher = publishers.save(Publisher.builder().name("P2 NXB Trẻ").build());
        Book book = books.save(Book.builder().title("P2 Hoa vàng").isbn("9786040000031").publisher(publisher)
                .publishYear((short) 2023).pageCount(378).language("vi").description("Childhood days. A gentle story.")
                .coverUrl("/images/home/cover-hoa-vang.jpg").publicationStatus(PublicationStatus.PUBLISHED)
                .authors(Set.of(nhatAnh)).categories(Set.of(novels)).build());
        copies.save(BookCopy.builder().book(book).barcode("P2-HV-01").status(CopyStatus.AVAILABLE).shelfLocation("Floor 1 · Shelf A2").build());
        BookCopy lent = copies.save(BookCopy.builder().book(book).barcode("P2-HV-02").status(CopyStatus.BORROWED).shelfLocation("Floor 1 · Shelf A2").build());
        loans.save(Loan.builder().copy(lent).member(member).dueDate(LocalDate.now().plusDays(3)).status(LoanStatus.BORROWING).build());
        copies.save(BookCopy.builder().book(book).barcode("P2-HV-03").status(CopyStatus.DAMAGED).build());

        String html = mvc.perform(as(member, get("/catalog/" + book.getId()))).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(html).contains("P2 Hoa vàng", "src=\"/images/home/cover-hoa-vang.jpg\"", "P2 Nguyễn Nhật Ánh", "P2 test novels",
                "Childhood days.", "9786040000031", "P2 NXB Trẻ", "Floor 1 · Shelf A2",
                "P2-HV-01", "On the shelf", "P2-HV-02", "On loan", "P2-HV-03", "Being repaired",
                "Borrow this book", "href=\"/loans/confirm?bookId=" + book.getId() + "\"");

        Book allOut = book("P2 All out", novels, nhatAnh, 2019, "vi", "9786040000032");
        borrowedCopies(allOut, 1);
        assertThat(mvc.perform(as(member, get("/catalog/" + allOut.getId()))).andReturn().getResponse().getContentAsString())
                .contains("All copies are out", "Reserve · coming soon").doesNotContain("Borrow this book");
    }

    // ------------------------------------------------------------------ 2.5 UC09

    @Test
    @DisplayName("2.5 UC09 AI assistant: conversation UI, suggested questions, send and reply, real catalog books (AI service not connected by decision)")
    void uc09() throws Exception {
        Book suggested = book("P2 Suggested read", novels, nhatAnh, 2021, "vi", "9786040000041");
        borrowedCopies(suggested, 3);
        String page = page("/assistant");
        assertThat(page).contains("Where would you like to start?", "Find your next read", "Suggest similar books",
                "P2 Suggested read", "href=\"/catalog/" + suggested.getId() + "\"", "UI example · AI is not connected");
        String sent = mvc.perform(post("/assistant").with(csrf()).param("message", "Gentle books for the weekend"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(sent).contains("Gentle books for the weekend", "The AI assistant is not connected yet",
                "href=\"/catalog?keyword=Gentle%20books%20for%20the%20weekend\"");
    }

    // ------------------------------------------------------------------ shared

    @Test
    @DisplayName("2.6 Shared navbar and footer for Guest and Member")
    void navigation() throws Exception {
        String guest = page("/catalog");
        assertThat(guest).contains("bn-navbar", "Explore books", "About the library", "Contact", "AI assistant",
                "Sign in", "Member registration", "bn-footer").doesNotContain("My books");
        String reader = mvc.perform(as(member, get("/catalog"))).andReturn().getResponse().getContentAsString();
        assertThat(reader).contains("bn-navbar", "AI assistant", "My books", "/account/profile", "bn-footer")
                .doesNotContain("Member registration");
    }

    // ------------------------------------------------------------------ helpers

    private Book book(String title, Category genre, Author author, int year, String language, String isbn) {
        return books.save(Book.builder().title(title).isbn(isbn).publishYear((short) year).language(language)
                .publicationStatus(PublicationStatus.PUBLISHED).categories(Set.of(genre)).authors(Set.of(author)).build());
    }

    private void copy(Book book, String barcode, CopyStatus status) {
        copies.save(BookCopy.builder().book(book).barcode(barcode).status(status).build());
    }

    private void borrowedCopies(Book book, int count) {
        for (int i = 0; i < count; i++) {
            BookCopy copy = copies.save(BookCopy.builder().book(book).barcode(book.getIsbn() + "-L" + i).status(CopyStatus.BORROWED).build());
            loans.save(Loan.builder().copy(copy).member(member).dueDate(LocalDate.now().plusDays(4)).status(LoanStatus.BORROWING).build());
        }
    }

    private String page(String path) throws Exception {
        return mvc.perform(get(path)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    }

    private String catalog(String... params) throws Exception {
        MockHttpServletRequestBuilder request = get("/catalog");
        for (int i = 0; i < params.length; i += 2) request = request.param(params[i], params[i + 1]);
        return mvc.perform(request).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    }

    private static MockHttpServletRequestBuilder as(User who, MockHttpServletRequestBuilder request) {
        return request.with(user(new CustomUserDetails(who)));
    }
}
