package grp3.java.booknet;

import grp3.java.booknet.entity.book.Book;
import grp3.java.booknet.entity.book.BookCopy;
import grp3.java.booknet.entity.book_service.Loan;
import grp3.java.booknet.entity.user.User;
import grp3.java.booknet.enums.book.CopyStatus;
import grp3.java.booknet.enums.book.PublicationStatus;
import grp3.java.booknet.enums.book_service.LoanStatus;
import grp3.java.booknet.enums.user.AuthProvider;
import grp3.java.booknet.enums.user.UserRole;
import grp3.java.booknet.enums.user.UserStatus;
import grp3.java.booknet.repository.book.BookCopyRepository;
import grp3.java.booknet.repository.book.BookRepository;
import grp3.java.booknet.repository.book_service.LoanRepository;
import grp3.java.booknet.repository.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** UC07 additions: publication year range and "Most popular" sort. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CatalogDiscoveryTests {
    @Autowired MockMvc mvc;
    @Autowired BookRepository books;
    @Autowired BookCopyRepository copies;
    @Autowired LoanRepository loans;
    @Autowired UserRepository users;

    @Test
    void yearRangeKeepsBooksInsideTheRangeAndSwapsReversedEnds() throws Exception {
        book("Range zz 1999", 1999);
        book("Range zz 2005", 2005);
        book("Range zz 2012", 2012);
        book("Range zz 2020", 2020);

        String inside = catalog("keyword", "Range zz", "yearFrom", "2004", "yearTo", "2013");
        assertThat(inside).contains("Range zz 2005", "Range zz 2012").doesNotContain("Range zz 1999", "Range zz 2020");
        String reversed = catalog("keyword", "Range zz", "yearFrom", "2013", "yearTo", "2004");
        assertThat(reversed).contains("Range zz 2005", "Range zz 2012").doesNotContain("Range zz 1999", "Range zz 2020");
        assertThat(catalog("keyword", "Range zz", "yearFrom", "2015")).contains("Range zz 2020").doesNotContain("Range zz 2012");
        // Links that still use the single-year filter keep working.
        assertThat(catalog("keyword", "Range zz", "publishYear", "1999")).contains("Range zz 1999").doesNotContain("Range zz 2005");
        assertThat(inside).contains("name=\"yearFrom\"", "name=\"yearTo\"");
    }

    @Test
    void mostPopularIsTheDefaultAndRanksByLoans() throws Exception {
        User reader = users.save(User.builder().fullName("Loan Reader").email("loans@example.com").role(UserRole.ROLE_USER)
                .provider(AuthProvider.LOCAL).status(UserStatus.ACTIVE).enabled(true).emailVerified(true).build());
        Book quiet = book("Popular zz quiet", 2001);
        Book loved = book("Popular zz loved", 2002);
        Book liked = book("Popular zz liked", 2003);
        borrow(loved, reader, 3);
        borrow(liked, reader, 1);

        String html = catalog("keyword", "Popular zz");
        assertThat(html).contains("<option value=\"popular\" selected=\"selected\">Most popular</option>");
        assertThat(html.indexOf("Popular zz loved")).isLessThan(html.indexOf("Popular zz liked"));
        assertThat(html.indexOf("Popular zz liked")).isLessThan(html.indexOf("Popular zz quiet"));

        String titles = catalog("keyword", "Popular zz", "sort", "title-asc");
        assertThat(titles.indexOf("Popular zz liked")).isLessThan(titles.indexOf("Popular zz loved"));
        assertThat(titles.indexOf("Popular zz loved")).isLessThan(titles.indexOf("Popular zz quiet"));
        assertThat(quiet.getId()).isNotNull();
    }

    private void borrow(Book book, User reader, int times) {
        for (int i = 0; i < times; i++) {
            BookCopy copy = copies.save(BookCopy.builder().book(book).barcode(book.getTitle().replace(' ', '-') + "-" + i)
                    .status(CopyStatus.BORROWED).build());
            loans.save(Loan.builder().copy(copy).member(reader).dueDate(LocalDate.now().plusDays(5)).status(LoanStatus.BORROWING).build());
        }
    }

    private Book book(String title, int year) {
        return books.save(Book.builder().title(title).publishYear((short) year).publicationStatus(PublicationStatus.PUBLISHED).build());
    }

    private String catalog(String... params) throws Exception {
        var request = get("/catalog");
        for (int i = 0; i < params.length; i += 2) request = request.param(params[i], params[i + 1]);
        return mvc.perform(request).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    }
}
