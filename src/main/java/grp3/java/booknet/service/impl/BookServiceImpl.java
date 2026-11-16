package grp3.java.booknet.service.impl;

import grp3.java.booknet.dto.book.request.BookCreateForm;
import grp3.java.booknet.dto.book.request.BookEditForm;
import grp3.java.booknet.dto.book.request.BookSearchFilter;
import grp3.java.booknet.dto.book.response.BookListRowDto;
import grp3.java.booknet.entity.book.Author;
import grp3.java.booknet.entity.book.Book;
import grp3.java.booknet.entity.book.Category;
import grp3.java.booknet.entity.book.Publisher;
import grp3.java.booknet.enums.book.CopyStatus;
import grp3.java.booknet.enums.book.PublicationStatus;
import grp3.java.booknet.repository.book.*;
import grp3.java.booknet.service.BookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Override
    @Transactional
    public Long createBook(BookCreateForm form) {
        if (bookRepository.existsByBookCode(form.getBookCode().trim())) {
            throw new IllegalArgumentException("Book code '" + form.getBookCode() + "' is already in use");
        }
        if (bookRepository.existsByIsbn(form.getIsbn().trim())) {
            throw new IllegalArgumentException("ISBN '" + form.getIsbn() + "' is already in use");
        }

        Set<Author> authors = new HashSet<>();
        if (form.getAuthorIds() != null && !form.getAuthorIds().isEmpty()) {
            for (Long authorId : form.getAuthorIds()) {
                Author author = authorRepository.findById(authorId)
                        .orElseThrow(() -> new IllegalArgumentException("Author not found with ID: " + authorId));
                authors.add(author);
            }
        }

        Set<Category> categories = new HashSet<>();
        if (form.getCategoryIds() != null && !form.getCategoryIds().isEmpty()) {
            for (Long catId : form.getCategoryIds()) {
                Category category = categoryRepository.findById(catId)
                        .orElseThrow(() -> new IllegalArgumentException("Category not found with ID: " + catId));
                categories.add(category);
            }
        }

        Publisher publisher = null;
        if (form.getPublisherId() != null) {
            publisher = publisherRepository.findById(form.getPublisherId())
                    .orElseThrow(() -> new IllegalArgumentException("Publisher not found with ID: " + form.getPublisherId()));
        }

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
                .coverUrl(form.getCoverUrl() != null ? form.getCoverUrl().trim() : null)
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

        List<Long> authorIds = book.getAuthors().stream()
                .map(a -> a.getId().longValue())
                .collect(Collectors.toList());

        List<Long> categoryIds = book.getCategories().stream()
                .map(c -> c.getId().longValue())
                .collect(Collectors.toList());

        Long publisherId = book.getPublisher() != null ? book.getPublisher().getId().longValue() : null;

        return BookEditForm.builder()
                .title(book.getTitle())
                .bookCode(book.getBookCode())
                .isbn(book.getIsbn())
                .authorIds(authorIds)
                .categoryIds(categoryIds)
                .publisherId(publisherId)
                .translator(book.getTranslator())
                .edition(book.getEdition())
                .language(book.getLanguage())
                .publishYear(book.getPublishYear())
                .pageCount(book.getPageCount())
                .description(book.getDescription())
                .keywords(book.getKeywords())
                .coverUrl(book.getCoverUrl())
                .build();
    }

    @Override
    @Transactional
    public void updateBook(Long id, BookEditForm form) {
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

        Set<Author> authors = new HashSet<>();
        if (form.getAuthorIds() != null && !form.getAuthorIds().isEmpty()) {
            for (Long authorId : form.getAuthorIds()) {
                Author author = authorRepository.findById(authorId)
                        .orElseThrow(() -> new IllegalArgumentException("Author not found with ID: " + authorId));
                authors.add(author);
            }
        }

        Set<Category> categories = new HashSet<>();
        if (form.getCategoryIds() != null && !form.getCategoryIds().isEmpty()) {
            for (Long catId : form.getCategoryIds()) {
                Category category = categoryRepository.findById(catId)
                        .orElseThrow(() -> new IllegalArgumentException("Category not found with ID: " + catId));
                categories.add(category);
            }
        }

        Publisher publisher = null;
        if (form.getPublisherId() != null) {
            publisher = publisherRepository.findById(form.getPublisherId())
                    .orElseThrow(() -> new IllegalArgumentException("Publisher not found with ID: " + form.getPublisherId()));
        }

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
        if (form.getCoverUrl() != null && !form.getCoverUrl().isBlank()) {
            book.setCoverUrl(form.getCoverUrl().trim());
        }

        bookRepository.save(book);
        log.info("Updated book ID: {}, code: {}", book.getId(), book.getBookCode());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BookListRowDto> getBookManagementList(BookSearchFilter filter, Pageable pageable) {
        Specification<Book> spec = BookSpecifications.withFilter(filter, false);
        Page<Book> bookPage = bookRepository.findAll(spec, pageable);

        if (bookPage.isEmpty()) {
            return Page.empty(pageable);
        }

        List<Long> bookIds = bookPage.getContent().stream()
                .map(Book::getId)
                .collect(Collectors.toList());

        // Batch count copies to avoid N+1 queries
        Map<Long, Long> totalCopiesMap = new HashMap<>();
        List<Object[]> totalCounts = bookCopyRepository.countTotalCopiesByBookIds(bookIds);
        for (Object[] row : totalCounts) {
            Long bookId = (Long) row[0];
            Long count = (Long) row[1];
            totalCopiesMap.put(bookId, count);
        }

        Map<Long, Long> availableCopiesMap = new HashMap<>();
        List<Object[]> availableCounts = bookCopyRepository.countCopiesByBookIdsAndStatus(bookIds, CopyStatus.AVAILABLE);
        for (Object[] row : availableCounts) {
            Long bookId = (Long) row[0];
            Long count = (Long) row[1];
            availableCopiesMap.put(bookId, count);
        }

        List<BookListRowDto> rows = bookPage.getContent().stream().map(book -> {
            List<String> authors = book.getAuthors().stream()
                    .map(Author::getName)
                    .sorted()
                    .collect(Collectors.toList());

            List<String> categories = book.getCategories().stream()
                    .map(Category::getName)
                    .sorted()
                    .collect(Collectors.toList());

            String publisherName = book.getPublisher() != null ? book.getPublisher().getName() : null;
            long totalCopies = totalCopiesMap.getOrDefault(book.getId(), 0L);
            long availableCopies = availableCopiesMap.getOrDefault(book.getId(), 0L);

            return BookListRowDto.builder()
                    .id(book.getId())
                    .bookCode(book.getBookCode())
                    .isbn(book.getIsbn())
                    .title(book.getTitle())
                    .authors(authors)
                    .categories(categories)
                    .publisherName(publisherName)
                    .publishYear(book.getPublishYear())
                    .language(book.getLanguage())
                    .publicationStatus(book.getPublicationStatus())
                    .deleted(book.isDeleted())
                    .totalCopies(totalCopies)
                    .availableCopies(availableCopies)
                    .build();
        }).collect(Collectors.toList());

        // If availableOnly filter is specified in memory or requested
        if (filter != null && Boolean.TRUE.equals(filter.availableOnly())) {
            List<BookListRowDto> filteredRows = rows.stream()
                    .filter(r -> r.availableCopies() > 0)
                    .collect(Collectors.toList());
            return new PageImpl<>(filteredRows, pageable, filteredRows.size());
        }

        return new PageImpl<>(rows, pageable, bookPage.getTotalElements());
    }
}
