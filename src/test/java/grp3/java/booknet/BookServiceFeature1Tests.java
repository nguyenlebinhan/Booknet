package grp3.java.booknet;

import grp3.java.booknet.dto.book.request.BookSearchFilter;
import grp3.java.booknet.dto.book.response.BookListRowDto;
import grp3.java.booknet.entity.book.*;
import grp3.java.booknet.enums.book.CopyStatus;
import grp3.java.booknet.enums.book.PublicationStatus;
import grp3.java.booknet.repository.book.*;
import grp3.java.booknet.service.BookService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class BookServiceFeature1Tests {

    @Autowired
    private BookService bookService;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private BookCopyRepository bookCopyRepository;

    @Autowired
    private AuthorRepository authorRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private PublisherRepository publisherRepository;

    private Book cleanCodeBook;
    private Book dddBook;
    private Category techCat;
    private Author robertMartin;

    @BeforeEach
    void setUp() {
        bookCopyRepository.deleteAll();
        bookRepository.deleteAll();
        categoryRepository.deleteAll();
        authorRepository.deleteAll();
        publisherRepository.deleteAll();

        Publisher pub = publisherRepository.save(Publisher.builder()
                .name("Prentice Hall")
                .build());

        techCat = categoryRepository.save(Category.builder()
                .name("Technology")
                .build());

        robertMartin = authorRepository.save(Author.builder()
                .name("Robert C. Martin")
                .build());

        cleanCodeBook = bookRepository.save(Book.builder()
                .title("Clean Code")
                .bookCode("BK-CLEAN-01")
                .isbn("978-0132350884")
                .publisher(pub)
                .publishYear((short) 2008)
                .language("en")
                .publicationStatus(PublicationStatus.PUBLISHED)
                .authors(Set.of(robertMartin))
                .categories(Set.of(techCat))
                .build());

        bookCopyRepository.save(BookCopy.builder()
                .book(cleanCodeBook)
                .barcode("CP-CC-001")
                .status(CopyStatus.AVAILABLE)
                .shelfLocation("Shelf A1")
                .build());

        bookCopyRepository.save(BookCopy.builder()
                .book(cleanCodeBook)
                .barcode("CP-CC-002")
                .status(CopyStatus.BORROWED)
                .shelfLocation("Shelf A1")
                .build());

        dddBook = bookRepository.save(Book.builder()
                .title("Domain-Driven Design")
                .bookCode("BK-DDD-01")
                .isbn("978-0321125217")
                .publisher(pub)
                .publishYear((short) 2003)
                .language("en")
                .publicationStatus(PublicationStatus.DRAFT)
                .categories(Set.of(techCat))
                .build());
    }

    @Test
    @DisplayName("Should retrieve paginated list with copy counts")
    void shouldRetrievePaginatedBooksWithInventoryCounts() {
        Page<BookListRowDto> page = bookService.getBookManagementList(
                new BookSearchFilter(null, null, null, null, null, null, null, null, "id", "asc"),
                PageRequest.of(0, 10)
        );

        assertThat(page.getTotalElements()).isEqualTo(2);
        BookListRowDto cc = page.getContent().stream()
                .filter(b -> b.bookCode().equals("BK-CLEAN-01"))
                .findFirst().orElseThrow();

        assertThat(cc.title()).isEqualTo("Clean Code");
        assertThat(cc.totalCopies()).isEqualTo(2);
        assertThat(cc.availableCopies()).isEqualTo(1);
        assertThat(cc.authors()).contains("Robert C. Martin");
        assertThat(cc.categories()).contains("Technology");
        assertThat(cc.publisherName()).isEqualTo("Prentice Hall");
    }

    @Test
    @DisplayName("Should filter books by keyword across title or bookCode")
    void shouldFilterBooksByKeyword() {
        BookSearchFilter filter = new BookSearchFilter("Clean", null, null, null, null, null, null, null, "id", "asc");
        Page<BookListRowDto> page = bookService.getBookManagementList(filter, PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().get(0).title()).isEqualTo("Clean Code");
    }

    @Test
    @DisplayName("Should filter books by ISBN")
    void shouldFilterBooksByIsbn() {
        BookSearchFilter filter = new BookSearchFilter(null, null, null, "978-0321125217", null, null, null, null, "id", "asc");
        Page<BookListRowDto> page = bookService.getBookManagementList(filter, PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().get(0).title()).isEqualTo("Domain-Driven Design");
    }

    @Test
    @DisplayName("Should filter books by Author")
    void shouldFilterBooksByAuthor() {
        BookSearchFilter filter = new BookSearchFilter(null, null, null, null, "Martin", null, null, null, "id", "asc");
        Page<BookListRowDto> page = bookService.getBookManagementList(filter, PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().get(0).title()).isEqualTo("Clean Code");
    }

    @Test
    @DisplayName("Should filter books by PublicationStatus")
    void shouldFilterBooksByPublicationStatus() {
        BookSearchFilter filter = new BookSearchFilter(null, null, null, null, null, null, PublicationStatus.DRAFT, null, "id", "asc");
        Page<BookListRowDto> page = bookService.getBookManagementList(filter, PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().get(0).title()).isEqualTo("Domain-Driven Design");
    }

    @Test
    @DisplayName("Should return empty page when no results match")
    void shouldReturnEmptyPageWhenNoMatch() {
        BookSearchFilter filter = new BookSearchFilter("NonExistentBookXYZ", null, null, null, null, null, null, null, "id", "asc");
        Page<BookListRowDto> page = bookService.getBookManagementList(filter, PageRequest.of(0, 10));

        assertThat(page.isEmpty()).isTrue();
        assertThat(page.getTotalElements()).isEqualTo(0);
    }
}
