package grp3.java.booknet;

import grp3.java.booknet.dto.book.request.BookCreateForm;
import grp3.java.booknet.entity.book.*;
import grp3.java.booknet.enums.book.PublicationStatus;
import grp3.java.booknet.repository.book.*;
import grp3.java.booknet.service.book.BookService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class BookServiceFeature2Tests {

    @Autowired
    private BookService bookService;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private AuthorRepository authorRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private PublisherRepository publisherRepository;

    private Author author1;
    private Category cat1;
    private Publisher pub1;

    @BeforeEach
    void setUp() {
        bookRepository.deleteAll();
        categoryRepository.deleteAll();
        authorRepository.deleteAll();
        publisherRepository.deleteAll();

        author1 = authorRepository.save(Author.builder().name("Martin Fowler").build());
        cat1 = categoryRepository.save(Category.builder().name("Software Engineering").build());
        pub1 = publisherRepository.save(Publisher.builder().name("Addison-Wesley").build());
    }

    @Test
    @DisplayName("Should successfully create a new book defaulting to DRAFT status")
    void shouldCreateNewBookWithDraftStatus() {
        BookCreateForm form = BookCreateForm.builder()
                .title("Refactoring")
                .bookCode("BK-REF-01")
                .isbn("978-0201485677")
                .authorIds(List.of(author1.getId()))
                .categoryIds(List.of(cat1.getId()))
                .publisherName("  ADDISON-WESLEY  ")
                .translator("John Doe")
                .edition("2nd")
                .language("en")
                .publishYear((short) 1999)
                .pageCount(448)
                .description("Improving the design of existing code")
                .keywords("refactoring, clean code, design patterns")
                .build();

        Long bookId = bookService.createBook(form);
        assertThat(bookId).isNotNull();

        Book saved = bookRepository.findById(bookId).orElseThrow();
        assertThat(saved.getTitle()).isEqualTo("Refactoring");
        assertThat(saved.getBookCode()).isEqualTo("BK-REF-01");
        assertThat(saved.getIsbn()).isEqualTo("978-0201485677");
        assertThat(saved.getPublicationStatus()).isEqualTo(PublicationStatus.DRAFT);
        assertThat(saved.isDeleted()).isFalse();
        assertThat(saved.getAuthors()).extracting(Author::getName).contains("Martin Fowler");
        assertThat(saved.getCategories()).extracting(Category::getName).contains("Software Engineering");
        assertThat(saved.getPublisher().getName()).isEqualTo("Addison-Wesley");
        assertThat(publisherRepository.findAll()).hasSize(1);
        assertThat(saved.getTranslator()).isEqualTo("John Doe");
    }

    @Test
    @DisplayName("Should fail when book code already exists")
    void shouldFailWhenBookCodeExists() {
        bookRepository.save(Book.builder()
                .title("Existing Book")
                .bookCode("BK-DUPLICATE")
                .isbn("978-1111111111")
                .publicationStatus(PublicationStatus.DRAFT)
                .build());

        BookCreateForm form = BookCreateForm.builder()
                .title("Another Book")
                .bookCode("BK-DUPLICATE")
                .isbn("978-2222222222")
                .authorIds(List.of(author1.getId()))
                .categoryIds(List.of(cat1.getId()))
                .publishYear((short) 2020)
                .pageCount(200)
                .build();

        assertThatThrownBy(() -> bookService.createBook(form))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Book code 'BK-DUPLICATE' is already in use");
    }

    @Test
    @DisplayName("Should fail when ISBN already exists")
    void shouldFailWhenIsbnExists() {
        bookRepository.save(Book.builder()
                .title("Existing Book")
                .bookCode("BK-ORIGINAL")
                .isbn("978-3333333333")
                .publicationStatus(PublicationStatus.DRAFT)
                .build());

        BookCreateForm form = BookCreateForm.builder()
                .title("New Book Duplicate ISBN")
                .bookCode("BK-DIFFERENT")
                .isbn("978-3333333333")
                .authorIds(List.of(author1.getId()))
                .categoryIds(List.of(cat1.getId()))
                .publishYear((short) 2020)
                .pageCount(200)
                .build();

        assertThatThrownBy(() -> bookService.createBook(form))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ISBN '978-3333333333' is already in use");
    }

    @Test
    @DisplayName("Should link selected existing author records to a book")
    void shouldLinkSelectedAuthorRecords() {
        Author secondAuthor = authorRepository.save(Author.builder().name("New Writer").build());
        BookCreateForm form = BookCreateForm.builder()
                .title("New Author Book")
                .bookCode("BK-AUTHOR")
                .isbn("978-4444444444")
                .authorIds(List.of(secondAuthor.getId(), author1.getId(), secondAuthor.getId()))
                .categoryIds(List.of(cat1.getId()))
                .publishYear((short) 2020)
                .pageCount(200)
                .build();

        Long bookId = bookService.createBook(form);

        assertThat(bookRepository.findById(bookId).orElseThrow().getAuthors())
                .extracting(Author::getName)
                .containsExactlyInAnyOrder("Martin Fowler", "New Writer");
        assertThat(authorRepository.findAll())
                .extracting(Author::getName)
                .containsExactlyInAnyOrder("Martin Fowler", "New Writer");
    }

    @Test
    @DisplayName("Should link selected author and reuse publisher without case-sensitive duplicates")
    void shouldReusePublisherAndAuthorNames() {
        BookCreateForm form = BookCreateForm.builder()
                .title("Publisher reuse test")
                .bookCode("BK-PUBLISHER-01")
                .isbn("978-4444444446")
                .authorIds(List.of(author1.getId()))
                .categoryIds(List.of(cat1.getId()))
                .publisherName("addison-wesley")
                .publishYear((short) 2020)
                .pageCount(200)
                .build();

        Long bookId = bookService.createBook(form);
        Book saved = bookRepository.findById(bookId).orElseThrow();

        assertThat(saved.getAuthors()).extracting(Author::getName).containsExactly("Martin Fowler");
        assertThat(saved.getPublisher().getName()).isEqualTo("Addison-Wesley");
        assertThat(authorRepository.findAll()).hasSize(1);
        assertThat(publisherRepository.findAll()).hasSize(1);
    }

    @Test
    @DisplayName("Should reject a book form without a selected author")
    void shouldRejectMissingAuthorSelection() {
        BookCreateForm form = BookCreateForm.builder()
                .title("No Author Book")
                .bookCode("BK-NOAUTH")
                .isbn("978-4444444445")
                .authorIds(List.of())
                .categoryIds(List.of(cat1.getId()))
                .publishYear((short) 2020)
                .pageCount(200)
                .build();

        assertThatThrownBy(() -> bookService.createBook(form))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("At least one author must be selected");
    }

    @Test
    @DisplayName("Should reject author IDs that do not exist")
    void shouldRejectUnknownAuthorIds() {
        BookCreateForm form = BookCreateForm.builder()
                .title("Unknown Author Book")
                .bookCode("BK-UNKNOWN-AUTHOR")
                .isbn("978-4444444447")
                .authorIds(List.of(Integer.MAX_VALUE))
                .categoryIds(List.of(cat1.getId()))
                .publishYear((short) 2020)
                .pageCount(200)
                .build();

        assertThatThrownBy(() -> bookService.createBook(form))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Author not found with ID");
        assertThat(authorRepository.findAll()).hasSize(1);
    }

    @Test
    @DisplayName("Should create a publisher entered as free text")
    void shouldCreatePublisherByName() {
        BookCreateForm form = BookCreateForm.builder()
                .title("Publisher Entry Book")
                .bookCode("BK-PUBLISHER")
                .isbn("978-4444444446")
                .authorIds(List.of(author1.getId()))
                .categoryIds(List.of(cat1.getId()))
                .publisherName("  New Publisher  ")
                .publishYear((short) 2020)
                .pageCount(200)
                .build();

        Long bookId = bookService.createBook(form);

        assertThat(bookRepository.findById(bookId).orElseThrow().getPublisher().getName())
                .isEqualTo("New Publisher");
        assertThat(publisherRepository.findAll()).hasSize(2);
    }
}
