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
import grp3.java.booknet.repository.catalog.CatalogStatsRepository;
import grp3.java.booknet.service.catalog.CatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CatalogServiceImpl implements CatalogService {

    public static final String POPULAR = "popular";

    private final BookRepository bookRepository;
    private final BookCopyRepository bookCopyRepository;
    private final CategoryRepository categoryRepository;
    private final CatalogStatsRepository catalogStats;

    @Override
    @Transactional(readOnly = true)
    public Page<CatalogBookView> search(CatalogFilter filter, Pageable pageable, boolean includeUnpublished) {
        Specification<Book> specification = CatalogSpecifications.withFilter(
                filter, includeUnpublished, categoryIdsWithDescendants(filter));
        Page<Book> books = filter != null && POPULAR.equals(filter.getSort())
                ? mostBorrowedFirst(specification, pageable)
                : bookRepository.findAll(specification, pageable);
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

    private List<Integer> categoryIdsWithDescendants(CatalogFilter filter) {
        if (filter == null || filter.getCategoryIds() == null || filter.getCategoryIds().isEmpty()) {
            return List.of();
        }

        Set<Integer> includedIds = new LinkedHashSet<>(filter.getCategoryIds());
        Set<Integer> parentIds = new LinkedHashSet<>(includedIds);
        while (!parentIds.isEmpty()) {
            Set<Integer> nextParentIds = categoryRepository.findAllByParent_IdInOrderByNameAsc(parentIds).stream()
                    .map(Category::getId)
                    .filter(includedIds::add)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            parentIds = nextParentIds;
        }
        return List.copyOf(includedIds);
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

    /**
     * "Most popular": books borrowed most often come first, then the newest. Loan counts cannot be
     * ordered inside the filter query (it uses DISTINCT joins), so the matching books are ranked here.
     */
    private Page<Book> mostBorrowedFirst(Specification<Book> specification, Pageable pageable) {
        List<Book> matching = bookRepository.findAll(specification, Sort.by(Sort.Order.desc("id")));
        if (matching.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, 0);
        }
        Map<Long, Long> loans = copyCountsByBook(catalogStats.countLoansByBook(matching.stream().map(Book::getId).toList()));
        List<Book> ranked = matching.stream()
                .sorted(Comparator.comparing((Book book) -> loans.getOrDefault(book.getId(), 0L)).reversed())
                .toList();
        int from = (int) Math.min(pageable.getOffset(), ranked.size());
        int to = Math.min(from + pageable.getPageSize(), ranked.size());
        return new PageImpl<>(ranked.subList(from, to), pageable, ranked.size());
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
