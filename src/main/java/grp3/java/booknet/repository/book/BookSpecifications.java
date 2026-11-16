package grp3.java.booknet.repository.book;

import grp3.java.booknet.dto.book.request.BookSearchFilter;
import grp3.java.booknet.entity.book.Author;
import grp3.java.booknet.entity.book.Book;
import grp3.java.booknet.entity.book.Category;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public final class BookSpecifications {

    private BookSpecifications() {}

    public static Specification<Book> withFilter(BookSearchFilter filter, boolean includeDeleted) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (!includeDeleted) {
                predicates.add(cb.isFalse(root.get("deleted")));
            }

            if (filter == null) {
                return cb.and(predicates.toArray(new Predicate[0]));
            }

            // Keyword: search across title, bookCode, isbn, description
            if (filter.keyword() != null && !filter.keyword().isBlank()) {
                String pattern = "%" + filter.keyword().trim().toLowerCase() + "%";
                Predicate titleLike = cb.like(cb.lower(root.get("title")), pattern);
                Predicate codeLike = cb.like(cb.lower(root.get("bookCode")), pattern);
                Predicate isbnLike = cb.like(cb.lower(root.get("isbn")), pattern);
                Predicate descLike = cb.like(cb.lower(root.get("description")), pattern);
                predicates.add(cb.or(titleLike, codeLike, isbnLike, descLike));
            }

            if (filter.title() != null && !filter.title().isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("title")), "%" + filter.title().trim().toLowerCase() + "%"));
            }

            if (filter.bookCode() != null && !filter.bookCode().isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("bookCode")), "%" + filter.bookCode().trim().toLowerCase() + "%"));
            }

            if (filter.isbn() != null && !filter.isbn().isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("isbn")), "%" + filter.isbn().trim().toLowerCase() + "%"));
            }

            if (filter.publicationStatus() != null) {
                predicates.add(cb.equal(root.get("publicationStatus"), filter.publicationStatus()));
            }

            if (filter.author() != null && !filter.author().isBlank()) {
                Join<Book, Author> authorJoin = root.join("authors", JoinType.INNER);
                predicates.add(cb.like(cb.lower(authorJoin.get("name")), "%" + filter.author().trim().toLowerCase() + "%"));
                query.distinct(true);
            }

            if (filter.categoryId() != null) {
                Join<Book, Category> categoryJoin = root.join("categories", JoinType.INNER);
                predicates.add(cb.equal(categoryJoin.get("id"), filter.categoryId()));
                query.distinct(true);
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
