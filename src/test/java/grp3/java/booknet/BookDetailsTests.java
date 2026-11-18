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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** UC08 Book details & shelf location: real pages for guests, members and staff. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class BookDetailsTests {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Autowired MockMvc mvc;
    @Autowired BookRepository books;
    @Autowired BookCopyRepository copies;
    @Autowired AuthorRepository authors;
    @Autowired CategoryRepository categories;
    @Autowired PublisherRepository publishers;
    @Autowired LoanRepository loans;
    @Autowired UserRepository users;

    Category literature;
    Author author;
    User member;
    User librarian;

    @BeforeEach
    void prepare() {
        literature = categories.save(Category.builder().name("Details test literature").build());
        author = authors.save(Author.builder().name("Nguyễn Nhật Ánh").build());
        member = save("Booknet Reader", "reader@example.com", UserRole.ROLE_USER);
        librarian = save("Library Staff", "staff@example.com", UserRole.ROLE_LIBRARIAN);
    }

    @Test
    void guestSeesFullDetailsShelfLocationAndCopyStatus() throws Exception {
        Publisher publisher = publishers.save(Publisher.builder().name("NXB Trẻ").build());
        Book book = books.save(Book.builder().title("Tôi thấy hoa vàng trên cỏ xanh").bookCode("VN-001").isbn("9786041223456")
                .publisher(publisher).publishYear((short) 2023).edition("Reprint").pageCount(378).language("vi")
                .description("Childhood days, clear and gentle. A story of childhood in a poor village.")
                .keywords("childhood, village").translator(null).coverUrl("/images/home/cover-hoa-vang.jpg")
                .publicationStatus(PublicationStatus.PUBLISHED).authors(Set.of(author)).categories(Set.of(literature)).build());
        copy(book, "VN001-06", CopyStatus.AVAILABLE, "Floor 1 · Shelf A2 · Section 03");
        copy(book, "VN001-07", CopyStatus.AVAILABLE, "Floor 1 · Shelf A2 · Section 03");
        BookCopy out = copy(book, "VN001-01", CopyStatus.BORROWED, null);
        LocalDate due = LocalDate.now().plusDays(4);
        loans.save(Loan.builder().copy(out).member(member).dueDate(due).status(LoanStatus.BORROWING).build());
        Book related = books.save(Book.builder().title("Mắt biếc").publicationStatus(PublicationStatus.PUBLISHED)
                .authors(Set.of(author)).categories(Set.of(literature)).build());

        String html = mvc.perform(get("/catalog/" + book.getId())).andExpect(status().isOk())
                .andExpect(view().name("catalog/details")).andReturn().getResponse().getContentAsString();
        assertThat(html).contains("Tôi thấy hoa vàng trên cỏ xanh", "Book ID VN-001", "DETAILS TEST LITERATURE",
                "Childhood days, clear and gentle.", "A story of childhood in a poor village.", "NXB Trẻ", "2023",
                "378 pages / Vietnamese", "9786041223456", "Floor 1 · Shelf A2 · Section 03", "NXB Trẻ · Reprint · 2023",
                "Ready for you", "Available · 2/3 copies", "Sign in to borrow", "Copy status (3)",
                "VN001-06 · VN001-07", "Earliest return: " + DATE.format(due), "Popular",
                "href=\"/catalog?keyword=childhood\"", "Mắt biếc", "href=\"/catalog/" + related.getId() + "\"")
                .doesNotContain("Borrow this book", "Edit book", "Only staff can see");
        write("book-details", page(member, "/catalog/" + book.getId()));
    }

    @Test
    void memberBorrowsThroughTheLoanConfirmationPage() throws Exception {
        Book book = published("Dế Mèn phiêu lưu ký");
        copy(book, "DM-01", CopyStatus.AVAILABLE, null);
        String html = page(member, "/catalog/" + book.getId());
        assertThat(html).contains("Borrow this book", "href=\"/loans/confirm?bookId=" + book.getId() + "\"",
                "Expected loan date", DATE.format(LocalDate.now()), DATE.format(LocalDate.now().plusDays(14)),
                "Ask a librarian").doesNotContain("Sign in to borrow");
    }

    @Test
    void allCopiesOnLoanShowsTheReturnDateAndNoBorrowButton() throws Exception {
        Book book = published("Nhà giả kim");
        BookCopy out = copy(book, "NGK-01", CopyStatus.BORROWED, null);
        LocalDate due = LocalDate.now().plusDays(2);
        loans.save(Loan.builder().copy(out).member(member).dueDate(due).status(LoanStatus.BORROWING).build());
        String html = page(member, "/catalog/" + book.getId());
        assertThat(html).contains("All copies are out", "On loan", "The earliest is due back on", DATE.format(due),
                "Reserve · coming soon").doesNotContain("Borrow this book");

        Book none = published("Sapiens");
        assertThat(page(member, "/catalog/" + none.getId())).contains("Not available to borrow", "Not in circulation")
                .doesNotContain("Borrow this book", "Reserve · coming soon");
    }

    @Test
    void unpublishedBooksAreHiddenFromReadersButShownToStaff() throws Exception {
        Book draft = books.save(Book.builder().title("Draft details book").publicationStatus(PublicationStatus.DRAFT).build());
        Book deleted = books.save(Book.builder().title("Deleted details book").publicationStatus(PublicationStatus.PUBLISHED)
                .deleted(true).build());
        mvc.perform(get("/catalog/" + draft.getId())).andExpect(status().isNotFound());
        mvc.perform(as(member, get("/catalog/" + draft.getId()))).andExpect(status().isNotFound());
        mvc.perform(as(librarian, get("/catalog/" + deleted.getId()))).andExpect(status().isNotFound());
        mvc.perform(get("/catalog/999999")).andExpect(status().isNotFound());

        String staff = page(librarian, "/catalog/" + draft.getId());
        assertThat(staff).contains("Draft details book", "Only staff can see this book", "Edit book",
                "href=\"/admin/books/" + draft.getId() + "/edit\"", "admin-sidebar");
    }

    @Test
    void catalogCardsOpenDetailsAndTextIsEscaped() throws Exception {
        Book book = books.save(Book.builder().title("Escape <b>test</b>").description("<script>alert(1)</script>")
                .publicationStatus(PublicationStatus.PUBLISHED).build());
        assertThat(mvc.perform(get("/catalog").param("keyword", "Escape")).andReturn().getResponse().getContentAsString())
                .contains("href=\"/catalog/" + book.getId() + "\"");
        assertThat(mvc.perform(get("/catalog/" + book.getId())).andReturn().getResponse().getContentAsString())
                .contains("Escape &lt;b&gt;test&lt;/b&gt;", "&lt;script&gt;alert(1)&lt;/script&gt;")
                .doesNotContain("<script>alert(1)</script>", "<b>test</b>");
    }

    /** Saves the rendered page under target/ui-review for a visual check, like the member management tests. */
    private void write(String name, String html) throws Exception {
        java.nio.file.Path output = java.nio.file.Path.of("target", "ui-review");
        java.nio.file.Files.createDirectories(output);
        String assets = java.nio.file.Path.of("src/main/resources/static").toAbsolutePath().toUri().toString();
        java.nio.file.Files.writeString(output.resolve(name + ".html"), html.replace("href=\"/css/", "href=\"" + assets + "css/")
                .replace("src=\"/images/", "src=\"" + assets + "images/").replace("src=\"/js/", "src=\"" + assets + "js/"));
    }

    private Book published(String title) {
        return books.save(Book.builder().title(title).publicationStatus(PublicationStatus.PUBLISHED)
                .authors(Set.of(author)).categories(Set.of(literature)).build());
    }

    private BookCopy copy(Book book, String barcode, CopyStatus status, String shelf) {
        return copies.save(BookCopy.builder().book(book).barcode(barcode).status(status).shelfLocation(shelf).build());
    }

    private String page(User who, String path) throws Exception {
        return mvc.perform(as(who, get(path))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    }

    private static MockHttpServletRequestBuilder as(User who, MockHttpServletRequestBuilder request) {
        return request.with(user(new CustomUserDetails(who)));
    }

    private User save(String name, String email, UserRole role) {
        return users.save(User.builder().fullName(name).email(email).role(role).provider(AuthProvider.LOCAL)
                .status(UserStatus.ACTIVE).enabled(true).emailVerified(true).build());
    }
}
