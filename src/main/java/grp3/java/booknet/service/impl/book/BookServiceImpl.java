package grp3.java.booknet.service.impl.book;

import grp3.java.booknet.dto.book.request.BookCreateForm;
import grp3.java.booknet.dto.book.request.BookEditForm;
import grp3.java.booknet.entity.book.Author;
import grp3.java.booknet.entity.book.Book;
import grp3.java.booknet.entity.book.Category;
import grp3.java.booknet.entity.book.Publisher;
import grp3.java.booknet.enums.book.PublicationStatus;
import grp3.java.booknet.repository.book.*;
import grp3.java.booknet.service.book.BookService;
import grp3.java.booknet.service.book.CoverImageStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookServiceImpl implements BookService {

    private final BookRepository bookRepository;
    private final BookCopyRepository bookCopyRepository;
    private final AuthorRepository authorRepository;
    private final CategoryRepository categoryRepository;
    private final PublisherRepository publisherRepository;
    private final CoverImageStorage coverImageStorage;

    @Override
    @Transactional
    public Long createBook(BookCreateForm form) {
        if (bookRepository.existsByBookCode(form.getBookCode().trim())) {
            throw new IllegalArgumentException("Book code '" + form.getBookCode() + "' is already in use");
        }
        if (bookRepository.existsByIsbn(form.getIsbn().trim())) {
            throw new IllegalArgumentException("ISBN '" + form.getIsbn() + "' is already in use");
        }

        Set<Author> authors = resolveAuthors(form.getAuthorIds());
        Set<Category> categories = resolveCategories(form.getCategoryIds());
        Publisher publisher = resolvePublisher(form.getPublisherName());
        String coverUrl = storeCoverImage(form.getCoverImage(), null);

        Book newBook = Book.builder()
                .title(form.getTitle().trim())
                .bookCode(form.getBookCode().trim().toUpperCase())
                .isbn(form.getIsbn().trim())
                .publisher(publisher)
                .authors(authors)
                .categories(categories)
                .translator(form.getTranslator() != null ? form.getTranslator().trim() : null)
                .edition(form.getEdition() != null ? form.getEdition().trim() : null)
                .language(form.getLanguage() != null ? form.getLanguage().trim() : "vi")
                .publishYear(form.getPublishYear())
                .pageCount(form.getPageCount())
                .description(form.getDescription() != null ? form.getDescription().trim() : null)
                .keywords(form.getKeywords() != null ? form.getKeywords().trim() : null)
                .coverUrl(coverUrl)
                .publicationStatus(PublicationStatus.DRAFT) // Default to DRAFT
                .deleted(false)
                .build();

        Book saved = bookRepository.save(newBook);
        log.info("Created new book ID: {}, code: {}, status: {}", saved.getId(), saved.getBookCode(), saved.getPublicationStatus());
        return saved.getId();
    }

    @Override
    @Transactional(readOnly = true)
    public BookEditForm getBookForEdit(Long id) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Book not found with ID: " + id));

        List<Integer> authorIds = book.getAuthors().stream()
                .map(Author::getId)
                .sorted()
                .toList();

        List<Integer> categoryIds = book.getCategories().stream()
                .map(Category::getId)
                .collect(Collectors.toList());

        String publisherName = book.getPublisher() != null ? book.getPublisher().getName() : null;

        return BookEditForm.builder()
                .title(book.getTitle())
                .bookCode(book.getBookCode())
                .isbn(book.getIsbn())
                .authorIds(authorIds)
                .categoryIds(categoryIds)
                .publisherName(publisherName)
                .translator(book.getTranslator())
                .edition(book.getEdition())
                .language(book.getLanguage())
                .publishYear(book.getPublishYear())
                .pageCount(book.getPageCount())
                .description(book.getDescription())
                .keywords(book.getKeywords())
                .build();
    }

    @Override
    @Transactional
    public void updateBook(Long id, BookEditForm form) {
        updateBookFields(id, form);
    }

    @Override
    @Transactional
    public void updateAndPublishBook(Long id, BookEditForm form) {
        updateBookFields(id, form);
        Book book = bookRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new IllegalArgumentException("Book not found with ID: " + id));
        if (book.getPublicationStatus() == PublicationStatus.DISCONTINUED) {
            throw new IllegalStateException("A discontinued book cannot be published.");
        }
        book.setPublicationStatus(PublicationStatus.PUBLISHED);
        log.info("Published book ID: {}, code: {}", book.getId(), book.getBookCode());
    }

    private void updateBookFields(Long id, BookEditForm form) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Book not found with ID: " + id));

        String trimmedCode = form.getBookCode().trim().toUpperCase();
        if (bookRepository.existsByBookCodeAndIdNot(trimmedCode, id)) {
            throw new IllegalArgumentException("Book code '" + form.getBookCode() + "' is already in use by another book");
        }

        String trimmedIsbn = form.getIsbn().trim();
        if (bookRepository.existsByIsbnAndIdNot(trimmedIsbn, id)) {
            throw new IllegalArgumentException("ISBN '" + form.getIsbn() + "' is already in use by another book");
        }

        Set<Author> authors = resolveAuthors(form.getAuthorIds());
        Set<Category> categories = resolveCategories(form.getCategoryIds());
        Publisher publisher = resolvePublisher(form.getPublisherName());

        book.setTitle(form.getTitle().trim());
        book.setBookCode(trimmedCode);
        book.setIsbn(trimmedIsbn);
        book.setAuthors(authors);
        book.setCategories(categories);
        book.setPublisher(publisher);
        book.setTranslator(form.getTranslator() != null ? form.getTranslator().trim() : null);
        book.setEdition(form.getEdition() != null ? form.getEdition().trim() : null);
        book.setLanguage(form.getLanguage() != null ? form.getLanguage().trim() : "vi");
        book.setPublishYear(form.getPublishYear());
        book.setPageCount(form.getPageCount());
        book.setDescription(form.getDescription() != null ? form.getDescription().trim() : null);
        book.setKeywords(form.getKeywords() != null ? form.getKeywords().trim() : null);
        book.setCoverUrl(storeCoverImage(form.getCoverImage(), book.getCoverUrl()));

        bookRepository.save(book);
        log.info("Updated book ID: {}, code: {}", book.getId(), book.getBookCode());
    }

    private Set<Author> resolveAuthors(List<Integer> authorIds) {
        if (authorIds == null || authorIds.isEmpty()) {
            throw new IllegalArgumentException("At least one author must be selected.");
        }

        Set<Integer> uniqueIds = new HashSet<>(authorIds);
        List<Author> authors = authorRepository.findAllById(uniqueIds);
        if (authors.size() != uniqueIds.size()) {
            Set<Integer> foundIds = authors.stream().map(Author::getId).collect(Collectors.toSet());
            uniqueIds.removeAll(foundIds);
            throw new IllegalArgumentException("Author not found with ID: " + uniqueIds.iterator().next());
        }
        return new HashSet<>(authors);
    }

    private Set<Category> resolveCategories(List<Integer> categoryIds) {
        if (categoryIds == null || categoryIds.isEmpty()) {
            throw new IllegalArgumentException("At least one category must be selected.");
        }
        Set<Category> categories = new HashSet<>();
        for (Integer categoryId : categoryIds) {
            Category category = categoryRepository.findById(categoryId)
                    .orElseThrow(() -> new IllegalArgumentException("Category not found with ID: " + categoryId));
            categories.add(category);
        }
        return categories;
    }

    private Publisher resolvePublisher(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        String normalized = name.trim();
        if (normalized.length() > 150) {
            throw new IllegalArgumentException("Publisher name cannot exceed 150 characters.");
        }
        return publisherRepository.findByNameIgnoreCase(normalized)
                .orElseGet(() -> publisherRepository.save(Publisher.builder().name(normalized).build()));
    }

    private String storeCoverImage(MultipartFile image, String currentCoverUrl) {
        if (image == null || image.isEmpty()) {
            return currentCoverUrl;
        }
        String storedUrl = coverImageStorage.store(image);
        registerCoverCleanup(storedUrl, currentCoverUrl);
        return storedUrl;
    }

    private void registerCoverCleanup(String newCoverUrl, String previousCoverUrl) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            coverImageStorage.delete(newCoverUrl);
            throw new IllegalStateException("Cover images must be stored within a database transaction.");
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_COMMITTED) {
                    if (previousCoverUrl != null && !previousCoverUrl.equals(newCoverUrl)) {
                        deleteCoverAfterTransaction(previousCoverUrl);
                    }
                } else {
                    deleteCoverAfterTransaction(newCoverUrl);
                }
            }
        });
    }

    private void deleteCoverAfterTransaction(String coverUrl) {
        try {
            coverImageStorage.delete(coverUrl);
        } catch (RuntimeException ex) {
            log.error("Failed to clean up cover image {} after book transaction.", coverUrl, ex);
        }
    }

}
