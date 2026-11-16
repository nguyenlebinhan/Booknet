package grp3.java.booknet;

import grp3.java.booknet.dto.book.request.BookEditForm;
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
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class BookServiceFeature3Tests {

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

    private Book existingBook;
    private Author author1;
    private Author author2;
    private Category cat1;
    private Category cat2;
    private Publisher pub1;
    private Publisher pub2;

    @BeforeEach
    void setUp() {
        bookRepository.deleteAll();
        categoryRepository.deleteAll();
        authorRepository.deleteAll();
        publisherRepository.deleteAll();

        author1 = authorRepository.save(Author.builder().name("Joshua Bloch").build());
        author2 = authorRepository.save(Author.builder().name("Neal Gafter").build());
        cat1 = categoryRepository.save(Category.builder().name("Java").build());
        cat2 = categoryRepository.save(Category.builder().name("Best Practices").build());
        pub1 = publisherRepository.save(Publisher.builder().name("Addison-Wesley").build());
        pub2 = publisherRepository.save(Publisher.builder().name("O'Reilly").build());

        existingBook = bookRepository.save(Book.builder()
                .title("Effective Java")
                .bookCode("BK-EJ-01")
                .isbn("978-0134685991")
                .authors(Set.of(author1))
                .categories(Set.of(cat1))
                .publisher(pub1)
                .publishYear((short) 2018)
                .pageCount(412)
                .language("en")
                .publicationStatus(PublicationStatus.DRAFT)
                .build());
    }

    @Test
    @DisplayName("Should successfully retrieve book data for editing")
    void shouldRetrieveBookForEdit() {
        BookEditForm editForm = bookService.getBookForEdit(existingBook.getId());

        assertThat(editForm.getTitle()).isEqualTo("Effective Java");
        assertThat(editForm.getBookCode()).isEqualTo("BK-EJ-01");
        assertThat(editForm.getIsbn()).isEqualTo("978-0134685991");
        assertThat(editForm.getAuthorIds()).contains(author1.getId().longValue());
        assertThat(editForm.getCategoryIds()).contains(cat1.getId().longValue());
        assertThat(editForm.getPublisherId()).isEqualTo(pub1.getId().longValue());
    }

    @Test
    @DisplayName("Should update book details, relationships, and preserve shared entities")
    void shouldUpdateBookDetails() {
        BookEditForm form = BookEditForm.builder()
                .title("Effective Java 3rd Edition")
                .bookCode("BK-EJ-03")
                .isbn("978-0134685991")
                .authorIds(List.of(author1.getId().longValue(), author2.getId().longValue()))
                .categoryIds(List.of(cat1.getId().longValue(), cat2.getId().longValue()))
                .publisherId(pub2.getId().longValue())
                .translator("Nguyen Van A")
                .edition("3rd")
                .language("vi")
                .publishYear((short) 2019)
                .pageCount(450)
                .description("Updated items for Java 7, 8, and 9")
                .keywords("java, best practices, bloch")
                .coverUrl("https://example.com/cover.jpg")
                .build();

        bookService.updateBook(existingBook.getId(), form);

        Book updated = bookRepository.findById(existingBook.getId()).orElseThrow();
        assertThat(updated.getTitle()).isEqualTo("Effective Java 3rd Edition");
        assertThat(updated.getBookCode()).isEqualTo("BK-EJ-03");
        assertThat(updated.getAuthors()).extracting(Author::getName).containsExactlyInAnyOrder("Joshua Bloch", "Neal Gafter");
        assertThat(updated.getCategories()).extracting(Category::getName).containsExactlyInAnyOrder("Java", "Best Practices");
        assertThat(updated.getPublisher().getName()).isEqualTo("O'Reilly");
        assertThat(updated.getTranslator()).isEqualTo("Nguyen Van A");

        // Verify shared authors and categories are not deleted
        assertThat(authorRepository.findAll()).hasSize(2);
        assertThat(categoryRepository.findAll()).hasSize(2);
    }

    @Test
    @DisplayName("Should fail when updating to a book code already taken by another book")
    void shouldFailWhenBookCodeTakenByAnotherBook() {
        bookRepository.save(Book.builder()
                .title("Other Book")
                .bookCode("BK-OTHER")
                .isbn("978-9999999999")
                .publicationStatus(PublicationStatus.DRAFT)
                .build());

        BookEditForm form = BookEditForm.builder()
                .title("Effective Java")
                .bookCode("BK-OTHER") // conflict
                .isbn("978-0134685991")
                .authorIds(List.of(author1.getId().longValue()))
                .categoryIds(List.of(cat1.getId().longValue()))
                .publishYear((short) 2018)
                .pageCount(412)
                .build();

        assertThatThrownBy(() -> bookService.updateBook(existingBook.getId(), form))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Book code 'BK-OTHER' is already in use by another book");
    }

    @Test
    @DisplayName("Should fail when book ID to update is not found")
    void shouldFailWhenBookNotFound() {
        BookEditForm form = BookEditForm.builder()
                .title("Non existent")
                .bookCode("BK-NEW")
                .isbn("978-0000000000")
                .authorIds(List.of(author1.getId().longValue()))
                .categoryIds(List.of(cat1.getId().longValue()))
                .publishYear((short) 2018)
                .pageCount(412)
                .build();

        assertThatThrownBy(() -> bookService.updateBook(99999L, form))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Book not found with ID: 99999");
    }
}
