package grp3.java.booknet.repository.book;

import grp3.java.booknet.dto.catalog.CatalogFilter;
import grp3.java.booknet.entity.book.Author;
import grp3.java.booknet.entity.book.Book;
import grp3.java.booknet.entity.book.BookCopy;
import grp3.java.booknet.entity.book.Category;
import grp3.java.booknet.enums.book.CopyStatus;
import grp3.java.booknet.enums.book.PublicationStatus;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class CatalogSpecifications {

    private static final Set<String> AVAILABILITY_FILTERS = Set.of("borrow", "reserve");
    private static final Set<String> SUPPORTED_LANGUAGES = Set.of("vi", "en");

    private CatalogSpecifications() {
    }

    public static Specification<Book> withFilter(CatalogFilter filter, boolean includeUnpublished) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isFalse(root.get("deleted")));
            if (!includeUnpublished) {
                predicates.add(cb.equal(root.get("publicationStatus"), PublicationStatus.PUBLISHED));
            }

            if (filter == null) {
                return cb.and(predicates.toArray(new Predicate[0]));
            }

            if (filter.getKeyword() != null && !filter.getKeyword().isBlank()) {
                String pattern = "%" + filter.getKeyword().trim().toLowerCase(Locale.ROOT) + "%";
                Join<Book, Author> authorJoin = root.join("authors", JoinType.LEFT);
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("title")), pattern),
                        cb.like(cb.lower(root.get("isbn")), pattern),
                        cb.like(cb.lower(root.get("bookCode")), pattern),
                        cb.like(cb.lower(authorJoin.get("name")), pattern)
                ));
                query.distinct(true);
            }

            if (filter.getCategoryIds() != null && !filter.getCategoryIds().isEmpty()) {
                Join<Book, Category> categoryJoin = root.join("categories", JoinType.INNER);
                predicates.add(categoryJoin.get("id").in(filter.getCategoryIds()));
                query.distinct(true);
            }

            if (filter.getLanguages() != null) {
                List<String> languages = filter.getLanguages().stream()
                        .filter(SUPPORTED_LANGUAGES::contains)
                        .toList();
                if (!languages.isEmpty()) {
                    predicates.add(cb.lower(root.get("language")).in(languages));
                }
            }

            if (filter.getPublishYear() != null) {
                predicates.add(cb.equal(root.get("publishYear"), filter.getPublishYear()));
            }
            if (filter.getYearFrom() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("publishYear"), filter.getYearFrom().shortValue()));
            }
            if (filter.getYearTo() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("publishYear"), filter.getYearTo().shortValue()));
            }

            List<String> availability = filter.getAvailability() == null
                    ? List.of()
                    : filter.getAvailability().stream().filter(AVAILABILITY_FILTERS::contains).distinct().toList();
            if (!availability.isEmpty()) {
                List<Predicate> availabilityPredicates = new ArrayList<>();
                if (availability.contains("borrow")) {
                    Subquery<Long> availableCount = query.subquery(Long.class);
                    Root<BookCopy> availableRoot = availableCount.from(BookCopy.class);
                    availableCount.select(cb.count(availableRoot))
                            .where(
                                    cb.equal(availableRoot.get("book"), root),
                                    cb.equal(availableRoot.get("status"), CopyStatus.AVAILABLE)
                            );
                    availabilityPredicates.add(cb.greaterThan(availableCount, 0L));
                }
                if (availability.contains("reserve")) {
                    Subquery<Long> reservableCount = query.subquery(Long.class);
                    Root<BookCopy> reservableRoot = reservableCount.from(BookCopy.class);
                    reservableCount.select(cb.count(reservableRoot))
                            .where(
                                    cb.equal(reservableRoot.get("book"), root),
                                    reservableRoot.get("status").in(CopyStatus.BORROWED, CopyStatus.RESERVED)
                            );

                    Subquery<Long> availableCount = query.subquery(Long.class);
                    Root<BookCopy> availableRoot = availableCount.from(BookCopy.class);
                    availableCount.select(cb.count(availableRoot))
                            .where(
                                    cb.equal(availableRoot.get("book"), root),
                                    cb.equal(availableRoot.get("status"), CopyStatus.AVAILABLE)
                            );
                    availabilityPredicates.add(cb.and(
                            cb.equal(availableCount, 0L),
                            cb.greaterThan(reservableCount, 0L)
                    ));
                }
                predicates.add(cb.or(availabilityPredicates.toArray(new Predicate[0])));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
