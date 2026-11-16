package grp3.java.booknet;

import grp3.java.booknet.entity.book.Author;
import grp3.java.booknet.entity.book.Book;
import grp3.java.booknet.entity.book.BookCopy;
import grp3.java.booknet.entity.book.Category;
import grp3.java.booknet.enums.book.CopyStatus;
import grp3.java.booknet.enums.book.PublicationStatus;
import grp3.java.booknet.repository.book.AuthorRepository;
import grp3.java.booknet.repository.book.BookCopyRepository;
import grp3.java.booknet.repository.book.BookRepository;
import grp3.java.booknet.repository.book.CategoryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CatalogControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private BookCopyRepository bookCopyRepository;

    @Autowired
    private AuthorRepository authorRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Test
    void publicCatalogSearchFiltersAndRendersBooks() throws Exception {
        Category category = new Category();
        category.setName("Catalog test literature");
        category = categoryRepository.save(category);

        Author author = new Author();
        author.setName("Catalog test author");
        author = authorRepository.save(author);

        Book book = new Book();
        book.setTitle("Catalog integration test book");
        book.setIsbn("9780000009001");
        book.setLanguage("en");
        book.setPublishYear((short) 2020);
        book.setPublicationStatus(PublicationStatus.PUBLISHED);
        book.setCategories(Set.of(category));
        book.setAuthors(Set.of(author));
        book = bookRepository.save(book);

        BookCopy copy = new BookCopy();
        copy.setBook(book);
        copy.setBarcode("CATALOG-TEST-AVAILABLE");
        copy.setStatus(CopyStatus.AVAILABLE);
        bookCopyRepository.save(copy);

        mockMvc.perform(get("/catalog")
                        .param("keyword", "Catalog test author")
                        .param("categoryIds", category.getId().toString())
                        .param("availability", "borrow")
                        .param("languages", "en")
                        .param("publishYear", "2020")
                        .param("sort", "title-asc"))
                .andExpect(status().isOk())
                .andExpect(view().name("catalog/index"))
                .andExpect(content().string(containsString("Catalog integration test book")))
                .andExpect(content().string(containsString("Available · 1/1 copies")))
                .andExpect(content().string(containsString("Catalog test literature")))
                .andExpect(content().string(not(containsString("Add a book"))))
                .andExpect(model().attribute("activePage", "catalog"));
    }

    @Test
    void publicCatalogShowsEmptyStateWhenFiltersMatchNoBooks() throws Exception {
        mockMvc.perform(get("/catalog").param("keyword", "no-catalog-match-expected"))
                .andExpect(status().isOk())
                .andExpect(view().name("catalog/index"))
                .andExpect(content().string(containsString("No books match these filters")));
    }

    @Test
    void publicCatalogDoesNotShowDraftBooks() throws Exception {
        bookRepository.save(Book.builder()
                .title("Catalog private draft book")
                .isbn("9780000009002")
                .publishYear((short) 1997)
                .publicationStatus(PublicationStatus.DRAFT)
                .build());

        mockMvc.perform(get("/catalog"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("Catalog private draft book"))));
    }

    @Test
    @WithMockUser(roles = "LIBRARIAN")
    void staffCatalogShowsNonPublishedBooksButNotDeletedBooks() throws Exception {
        bookRepository.save(Book.builder()
                .title("Catalog staff draft book")
                .isbn("9780000009003")
                .publishYear((short) 1997)
                .publicationStatus(PublicationStatus.DRAFT)
                .build());
        bookRepository.save(Book.builder()
                .title("Catalog staff discontinued book")
                .isbn("9780000009004")
                .publishYear((short) 1998)
                .publicationStatus(PublicationStatus.DISCONTINUED)
                .build());
        bookRepository.save(Book.builder()
                .title("Catalog staff deleted book")
                .isbn("9780000009005")
                .publicationStatus(PublicationStatus.DRAFT)
                .deleted(true)
                .build());

        mockMvc.perform(get("/catalog"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Catalog staff draft book")))
                .andExpect(content().string(containsString("Catalog staff discontinued book")))
                .andExpect(content().string(containsString("class=\"catalog-staff-link\"")))
                .andExpect(content().string(containsString("Add a book")))
                .andExpect(content().string(containsString("Staff catalog")))
                .andExpect(content().string(containsString("Draft")))
                .andExpect(content().string(containsString("Discontinued")))
                .andExpect(content().string(containsString(">1997</option>")))
                .andExpect(content().string(not(containsString("Catalog staff deleted book"))));
    }

    @Test
    void catalogPaginationKeepsPageNavigationAvailable() throws Exception {
        Category category = new Category();
        category.setName("Catalog pagination genre");
        category = categoryRepository.save(category);

        for (int i = 1; i <= 7; i++) {
            Book book = new Book();
            book.setTitle("Catalog pagination book " + i);
            book.setIsbn("9780000010" + String.format("%03d", i));
            book.setLanguage("en");
            book.setPublishYear((short) 2020);
            book.setPublicationStatus(PublicationStatus.PUBLISHED);
            book.setCategories(Set.of(category));
            book = bookRepository.save(book);

            BookCopy copy = new BookCopy();
            copy.setBook(book);
            copy.setBarcode("CATALOG-PAGE-" + i);
            copy.setStatus(CopyStatus.AVAILABLE);
            bookCopyRepository.save(copy);
        }

        mockMvc.perform(get("/catalog")
                        .param("sort", "title-asc")
                        .param("categoryIds", category.getId().toString())
                        .param("availability", "borrow")
                        .param("languages", "en")
                        .param("publishYear", "2020"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<span>1</span>–<span>6</span>")))
                .andExpect(content().string(containsString("of <span>7</span> results")))
                .andExpect(content().string(containsString("aria-label=\"Next page\"")))
                .andExpect(content().string(containsString("categoryIds=" + category.getId())))
                .andExpect(content().string(containsString("availability=borrow")))
                .andExpect(content().string(containsString("languages=en")))
                .andExpect(content().string(containsString("Catalog pagination book 1")));

        mockMvc.perform(get("/catalog")
                        .param("page", "1")
                        .param("sort", "title-asc")
                        .param("categoryIds", category.getId().toString())
                        .param("availability", "borrow")
                        .param("languages", "en")
                        .param("publishYear", "2020"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<span>7</span>–<span>7</span>")))
                .andExpect(content().string(containsString("aria-label=\"Previous page\"")))
                .andExpect(content().string(containsString("Catalog pagination book 7")));
    }
}
