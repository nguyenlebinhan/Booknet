package grp3.java.booknet.service.impl.catalog;

import grp3.java.booknet.dto.catalog.CatalogBookView;
import grp3.java.booknet.dto.catalog.CatalogFilter;
import grp3.java.booknet.entity.book.Book;
import grp3.java.booknet.entity.book.Category;
import grp3.java.booknet.enums.book.CopyStatus;
import grp3.java.booknet.enums.book.PublicationStatus;
import grp3.java.booknet.repository.book.BookCopyRepository;
import grp3.java.booknet.repository.book.BookRepository;
import grp3.java.booknet.repository.book.CatalogSpecifications;
import grp3.java.booknet.repository.book.CategoryRepository;
import grp3.java.booknet.service.catalog.CatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CatalogServiceImpl implements CatalogService {

    private final BookRepository bookRepository;
    private final BookCopyRepository bookCopyRepository;
    private final CategoryRepository categoryRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<CatalogBookView> search(CatalogFilter filter, Pageable pageable, boolean includeUnpublished) {
        Page<Book> books = bookRepository.findAll(CatalogSpecifications.withFilter(filter, includeUnpublished), pageable);
        List<Long> bookIds = books.getContent().stream().map(Book::getId).toList();
        if (bookIds.isEmpty()) {
            return books.map(book -> toView(book, 0, 0, false));
        }

        Map<Long, Long> totalCopies = copyCountsByBook(bookCopyRepository.countTotalCopiesByBookIds(bookIds));
        Map<Long, Long> availableCopies = copyCountsByBook(bookCopyRepository.countCopiesByBookIdsAndStatus(bookIds, CopyStatus.AVAILABLE));
        Map<Long, Long> reservableCopies = copyCountsByBook(bookCopyRepository.countCopiesByBookIdsAndStatuses(
                bookIds, List.of(CopyStatus.BORROWED, CopyStatus.RESERVED)));
        return books.map(book -> {
            long total = totalCopies.getOrDefault(book.getId(), 0L);
            long available = availableCopies.getOrDefault(book.getId(), 0L);
            return toView(book, total, available, reservableCopies.getOrDefault(book.getId(), 0L) > 0);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public List<Category> getCategories() {
        return categoryRepository.findAll(Sort.by(Sort.Direction.ASC, "name"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Short> getPublishYears(boolean includeUnpublished) {
        return includeUnpublished
                ? bookRepository.findDistinctPublishYears()
                : bookRepository.findDistinctPublishedYears(PublicationStatus.PUBLISHED);
    }

    private CatalogBookView toView(Book book, long totalCopies, long availableCopies, boolean hasReservableCopies) {
        boolean canReserve = availableCopies == 0 && hasReservableCopies;
        return new CatalogBookView(
                book.getId(),
                book.getTitle(),
                book.getAuthors().stream().map(author -> author.getName()).sorted().toList(),
                book.getCategories().stream().map(Category::getName).sorted().toList(),
                book.getCoverUrl(),
                book.getLanguage(),
                book.getPublishYear(),
                totalCopies,
                availableCopies,
                canReserve,
                book.getPublicationStatus()
        );
    }

    private static Map<Long, Long> copyCountsByBook(List<Object[]> rows) {
        return rows.stream().collect(Collectors.toMap(
                row -> ((Number) row[0]).longValue(),
                row -> ((Number) row[1]).longValue()
        ));
    }

}
