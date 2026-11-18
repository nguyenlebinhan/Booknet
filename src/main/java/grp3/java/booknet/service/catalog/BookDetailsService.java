package grp3.java.booknet.service.catalog;

import grp3.java.booknet.dto.catalog.BookDetailsView;
import grp3.java.booknet.dto.catalog.CatalogBookView;
import grp3.java.booknet.entity.book.Author;
import grp3.java.booknet.entity.book.Book;
import grp3.java.booknet.entity.book.BookCopy;
import grp3.java.booknet.entity.book.Category;
import grp3.java.booknet.enums.book.CopyStatus;
import grp3.java.booknet.enums.book.PublicationStatus;
import grp3.java.booknet.enums.book_service.LoanStatus;
import grp3.java.booknet.repository.book.BookCopyRepository;
import grp3.java.booknet.repository.book.BookRepository;
import grp3.java.booknet.repository.catalog.CatalogStatsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/** Builds the public Book details page (UC08) from the catalog's books, copies and loans. Read-only. */
@Service
@RequiredArgsConstructor
public class BookDetailsService {
    /** A book among this many most-borrowed titles gets the "Popular" badge. */
    static final int POPULAR_TOP = 5;
    static final int RELATED_LIMIT = 2;
    private static final Set<LoanStatus> ON_LOAN = Set.of(LoanStatus.BORROWING, LoanStatus.OVERDUE);

    private final BookRepository books;
    private final BookCopyRepository copies;
    private final CatalogStatsRepository stats;

    /**
     * Guests and members see published books only; staff also see drafts and discontinued books.
     * Deleted books, and anything a reader may not see, answer 404 so their existence is not revealed.
     */
    @Transactional(readOnly = true)
    public BookDetailsView details(Long bookId, boolean staff) {
        Book book = books.findById(bookId)
                .filter(b -> !b.isDeleted())
                .filter(b -> staff || b.getPublicationStatus() == PublicationStatus.PUBLISHED)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        List<BookCopy> bookCopies = copies.findByBook_Id(bookId);
        List<BookCopy> available = bookCopies.stream().filter(c -> c.getStatus() == CopyStatus.AVAILABLE)
                .sorted(Comparator.comparing(BookCopy::getBarcode)).toList();
        long onLoan = bookCopies.stream().filter(c -> c.getStatus() == CopyStatus.BORROWED).count();
        String shelf = (available.isEmpty() ? bookCopies : available).stream().map(BookCopy::getShelfLocation)
                .filter(s -> s != null && !s.isBlank()).findFirst().orElse(null);
        boolean popular = stats.findMostBorrowedBookIds(PublicationStatus.PUBLISHED, PageRequest.of(0, POPULAR_TOP)).contains(bookId);
        LocalDate today = LocalDate.now();

        return new BookDetailsView(book.getId(), book.getBookCode(), book.getTitle(),
                book.getAuthors().stream().map(Author::getName).sorted().toList(), blankToNull(book.getTranslator()),
                book.getCategories().stream().map(Category::getName).sorted().toList(), blankToNull(book.getDescription()),
                keywords(book.getKeywords()), blankToNull(book.getCoverUrl()),
                book.getPublisher() != null ? book.getPublisher().getName() : null, book.getPublishYear(),
                blankToNull(book.getEdition()), book.getPageCount(), languageName(book.getLanguage()), blankToNull(book.getIsbn()),
                shelf, book.getPublicationStatus(), bookCopies.size(), available.size(), onLoan,
                bookCopies.size() - available.size() - onLoan, available.stream().map(BookCopy::getBarcode).toList(),
                bookCopies.stream().sorted(Comparator.comparing(BookCopy::getBarcode)).map(BookDetailsService::row).toList(),
                onLoan > 0 ? stats.findEarliestDueDate(bookId, ON_LOAN) : null, popular,
                today, today.plusDays(BookDetailsView.LOAN_DAYS), related(book));
    }

    private List<CatalogBookView> related(Book book) {
        List<Integer> categoryIds = book.getCategories().stream().map(Category::getId).toList();
        List<Integer> authorIds = book.getAuthors().stream().map(Author::getId).toList();
        if (categoryIds.isEmpty() && authorIds.isEmpty()) return List.of();
        // An empty IN list is not valid SQL everywhere, so an impossible id stands in.
        List<Book> found = stats.findRelated(book.getId(), categoryIds.isEmpty() ? List.of(-1) : categoryIds,
                authorIds.isEmpty() ? List.of(-1) : authorIds, PublicationStatus.PUBLISHED, PageRequest.of(0, RELATED_LIMIT));
        if (found.isEmpty()) return List.of();
        List<Long> ids = found.stream().map(Book::getId).toList();
        Map<Long, Long> total = counts(copies.countTotalCopiesByBookIds(ids));
        Map<Long, Long> availableCounts = counts(copies.countCopiesByBookIdsAndStatus(ids, CopyStatus.AVAILABLE));
        Map<Long, Long> outCounts = counts(copies.countCopiesByBookIdsAndStatuses(ids, List.of(CopyStatus.BORROWED, CopyStatus.RESERVED)));
        return found.stream().map(b -> {
            long availableCount = availableCounts.getOrDefault(b.getId(), 0L);
            return new CatalogBookView(b.getId(), b.getTitle(), b.getAuthors().stream().map(Author::getName).sorted().toList(),
                    b.getCategories().stream().map(Category::getName).sorted().toList(), b.getCoverUrl(), b.getLanguage(),
                    b.getPublishYear(), total.getOrDefault(b.getId(), 0L), availableCount,
                    availableCount == 0 && outCounts.getOrDefault(b.getId(), 0L) > 0, b.getPublicationStatus());
        }).toList();
    }

    private static BookDetailsView.CopyRow row(BookCopy copy) {
        String[] label = switch (copy.getStatus()) {
            case AVAILABLE -> new String[]{"On the shelf", ""};
            case BORROWED -> new String[]{"On loan", "on-loan"};
            case RESERVED -> new String[]{"Held for a reader", "on-loan"};
            case DAMAGED -> new String[]{"Being repaired", "muted"};
            case LOST -> new String[]{"Missing", "muted"};
            case DISCONTINUED -> new String[]{"Withdrawn", "muted"};
        };
        return new BookDetailsView.CopyRow(copy.getBarcode(), label[0], label[1], blankToNull(copy.getShelfLocation()));
    }

    private static Map<Long, Long> counts(List<Object[]> rows) {
        return rows.stream().collect(Collectors.toMap(r -> ((Number) r[0]).longValue(), r -> ((Number) r[1]).longValue()));
    }

    private static List<String> keywords(String value) {
        if (value == null || value.isBlank()) return List.of();
        return Arrays.stream(value.split(",")).map(String::strip).filter(s -> !s.isEmpty()).distinct().toList();
    }

    static String languageName(String code) {
        if (code == null || code.isBlank()) return null;
        return switch (code) {
            case "vi" -> "Vietnamese";
            case "en" -> "English";
            case "other" -> "Other";
            default -> code;
        };
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
