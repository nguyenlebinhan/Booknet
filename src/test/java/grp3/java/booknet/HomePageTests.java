package grp3.java.booknet;

import grp3.java.booknet.dto.home.HomeView;
import grp3.java.booknet.entity.book.Author;
import grp3.java.booknet.entity.book.Book;
import grp3.java.booknet.entity.book.BookCopy;
import grp3.java.booknet.entity.book.Category;
import grp3.java.booknet.entity.book_service.Loan;
import grp3.java.booknet.entity.user.User;
import grp3.java.booknet.enums.book.CopyStatus;
import grp3.java.booknet.enums.book.PublicationStatus;
import grp3.java.booknet.enums.book_service.LoanStatus;
import grp3.java.booknet.enums.user.AuthProvider;
import grp3.java.booknet.enums.user.UserRole;
import grp3.java.booknet.enums.user.UserStatus;
import grp3.java.booknet.repository.book.AuthorRepository;
import grp3.java.booknet.repository.book.BookCopyRepository;
import grp3.java.booknet.repository.book.BookRepository;
import grp3.java.booknet.repository.book.CategoryRepository;
import grp3.java.booknet.repository.book_service.LoanRepository;
import grp3.java.booknet.repository.user.UserRepository;
import grp3.java.booknet.service.home.HomeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** UC05 public portal: the home page shows real catalog figures. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class HomePageTests {
    @Autowired MockMvc mvc;
    @Autowired BookRepository books;
    @Autowired BookCopyRepository copies;
    @Autowired CategoryRepository categories;
    @Autowired AuthorRepository authors;
    @Autowired LoanRepository loans;
    @Autowired UserRepository users;

    @Test
    void homeShowsRealGenresShelfAndQuickSearches() throws Exception {
        Category stories = categories.save(Category.builder().name("Home test stories").build());
        Category science = categories.save(Category.builder().name("Home test science").build());
        Author author = authors.save(Author.builder().name("Home Test Author").build());
        User reader = users.save(User.builder().fullName("Home Reader").email("home-reader@example.com").role(UserRole.ROLE_USER)
                .provider(AuthProvider.LOCAL).status(UserStatus.ACTIVE).enabled(true).emailVerified(true).build());
        Book loved = published("Home test loved book", Set.of(stories), author);
        published("Home test second story", Set.of(stories), author);
        Book quiet = published("Home test science book", Set.of(science), author);
        copies.save(BookCopy.builder().book(quiet).barcode("HOME-Q-1").status(CopyStatus.AVAILABLE).build());
        for (int i = 0; i < 3; i++) {
            BookCopy copy = copies.save(BookCopy.builder().book(loved).barcode("HOME-L-" + i).status(CopyStatus.BORROWED).build());
            loans.save(Loan.builder().copy(copy).member(reader).dueDate(LocalDate.now().plusDays(3)).status(LoanStatus.BORROWING).build());
        }

        String html = mvc.perform(get("/")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(html).contains("Home test stories", "2 titles", "href=\"/catalog?categoryIds=" + stories.getId() + "\"",
                "Home test science", "1 title",
                "Home test loved book", "href=\"/catalog/" + loved.getId() + "\"", "On loan",
                "Home test science book", "Available · 1/1 copies",
                "href=\"/catalog?keyword=Home%20Test%20Author\"")
                .doesNotContain("4.260 titles", "12.000+");
        assertThat(html.indexOf("Home test loved book")).isLessThan(html.indexOf("Home test science book"));
        assertThat(html).containsPattern("booknet (welcomes you until|opens at) \\d{2}:\\d{2}");
        write("home", html);
    }

    /** Saves the rendered page under target/ui-review for a visual check. */
    private static void write(String name, String html) throws Exception {
        java.nio.file.Path output = java.nio.file.Path.of("target", "ui-review");
        java.nio.file.Files.createDirectories(output);
        String assets = java.nio.file.Path.of("src/main/resources/static").toAbsolutePath().toUri().toString();
        java.nio.file.Files.writeString(output.resolve(name + ".html"), html.replace("href=\"/css/", "href=\"" + assets + "css/")
                .replace("src=\"/images/", "src=\"" + assets + "images/").replace("src=\"/js/", "src=\"" + assets + "js/"));
    }

    @Test
    void homeSubcategoryLinkOpensCatalogWithThatGenreSelected() throws Exception {
        Category parent = categories.save(Category.builder().name("Home test parent").build());
        Category child = categories.save(Category.builder().name("Home test subcategory").parent(parent).build());
        Author author = authors.save(Author.builder().name("Home subcategory author").build());
        Book childBook = published("Home subcategory book", Set.of(child), author);

        String home = mvc.perform(get("/")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(home).contains("href=\"/catalog?categoryIds=" + child.getId() + "\"");

        mvc.perform(get("/catalog").param("categoryIds", child.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString(childBook.getTitle())))
                .andExpect(model().attribute("filter",
                        org.hamcrest.Matchers.hasProperty("categoryIds",
                                org.hamcrest.Matchers.contains(child.getId()))));
    }

    @Test
    void openingStatusFollowsTheLibraryHours() throws Exception {
        Method opening = HomeService.class.getDeclaredMethod("opening", LocalDateTime.class);
        opening.setAccessible(true);
        // 16/11/2026 is a Monday; 21/11/2026 a Saturday; 22/11/2026 a Sunday.
        assertThat(headline(opening, LocalDateTime.of(2026, 11, 16, 10, 0))).isEqualTo("booknet welcomes you until 20:00 today");
        assertThat(headline(opening, LocalDateTime.of(2026, 11, 16, 7, 30))).isEqualTo("booknet opens at 08:00 today");
        assertThat(headline(opening, LocalDateTime.of(2026, 11, 16, 20, 0))).isEqualTo("booknet opens at 08:00 tomorrow");
        assertThat(headline(opening, LocalDateTime.of(2026, 11, 21, 21, 0))).isEqualTo("booknet opens at 09:00 tomorrow");
        assertThat(headline(opening, LocalDateTime.of(2026, 11, 22, 12, 0))).isEqualTo("booknet welcomes you until 17:00 today");
        assertThat(headline(opening, LocalDateTime.of(2026, 11, 22, 17, 30))).isEqualTo("booknet opens at 08:00 tomorrow");
    }

    private static String headline(Method opening, LocalDateTime at) throws Exception {
        return ((HomeView.OpeningStatus) opening.invoke(null, at)).headline();
    }

    private Book published(String title, Set<Category> genres, Author author) {
        return books.save(Book.builder().title(title).publicationStatus(PublicationStatus.PUBLISHED)
                .categories(genres).authors(Set.of(author)).build());
    }
}
