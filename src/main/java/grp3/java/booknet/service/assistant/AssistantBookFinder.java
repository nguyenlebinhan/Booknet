package grp3.java.booknet.service.assistant;

import grp3.java.booknet.dto.catalog.CatalogBookView;
import grp3.java.booknet.dto.catalog.CatalogFilter;
import grp3.java.booknet.entity.book.Book;
import grp3.java.booknet.enums.book.PublicationStatus;
import grp3.java.booknet.repository.book.BookRepository;
import grp3.java.booknet.service.catalog.CatalogService;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Picks the published books the assistant may talk about for one question. */
@Component
@RequiredArgsConstructor
public class AssistantBookFinder {
    private static final int MAX_WORDS = 8;
    private static final int MAX_MATCHES = 12;
    private static final int POPULAR = 8;
    private static final int DESCRIPTION = 240;
    private static final Set<String> COMMON_WORDS = Set.of("the", "and", "for", "with", "about", "book", "books", "can",
            "could", "you", "your", "any", "some", "suggest", "recommend", "find", "read", "reading", "what", "which",
            "where", "how", "who", "are", "have", "has", "please", "want", "like", "similar", "that", "this", "from", "there",
            "does", "library", "borrow", "sách", "cho", "tôi", "của", "những", "các", "cuốn", "một", "nào", "hãy", "gợi", "về");

    private final BookRepository books;
    private final CatalogService catalog;

    /** @param matched true when words of the question appear in the book; false for popular or earlier books */
    public record Candidate(long id, boolean matched, String summary) {
    }

    @Transactional(readOnly = true)
    public List<Candidate> find(String question, Collection<Long> earlierBookIds) {
        List<String> words = words(question);
        List<Book> matches = words.isEmpty() ? List.of()
                : books.findAll(anyWord(words), PageRequest.of(0, 100, Sort.by(Sort.Direction.DESC, "id"))).stream()
                .sorted(Comparator.comparingInt((Book book) -> score(book, words)).reversed())
                .limit(MAX_MATCHES).toList();
        Set<Long> matchedIds = matches.stream().map(Book::getId).collect(Collectors.toSet());

        Set<Long> ids = new LinkedHashSet<>(matches.stream().map(Book::getId).toList());
        ids.addAll(earlierBookIds);
        CatalogFilter popular = new CatalogFilter();
        popular.setSort("popular");
        catalog.search(popular, PageRequest.of(0, POPULAR), false).forEach(book -> ids.add(book.id()));

        Map<Long, Book> entities = books.findAllById(ids).stream()
                .collect(Collectors.toMap(Book::getId, Function.identity()));
        return catalog.findPublished(ids).stream()
                .map(view -> new Candidate(view.id(), matchedIds.contains(view.id()), summary(view, entities.get(view.id()))))
                .toList();
    }

    private static Specification<Book> anyWord(List<String> words) {
        return (root, query, cb) -> {
            query.distinct(true);
            var author = root.join("authors", JoinType.LEFT);
            var category = root.join("categories", JoinType.LEFT);
            List<Predicate> any = new ArrayList<>();
            for (String word : words) {
                String pattern = "%" + word + "%";
                any.add(cb.like(cb.lower(root.get("title")), pattern));
                any.add(cb.like(cb.lower(root.get("keywords")), pattern));
                any.add(cb.like(cb.lower(root.get("description")), pattern));
                any.add(cb.like(root.get("isbn"), pattern));
                any.add(cb.like(cb.lower(root.get("bookCode")), pattern));
                any.add(cb.like(cb.lower(author.get("name")), pattern));
                any.add(cb.like(cb.lower(category.get("name")), pattern));
            }
            return cb.and(cb.isFalse(root.get("deleted")),
                    cb.equal(root.get("publicationStatus"), PublicationStatus.PUBLISHED),
                    cb.or(any.toArray(new Predicate[0])));
        };
    }

    private static int score(Book book, List<String> words) {
        String strong = text(book.getTitle(), book.getIsbn(), book.getBookCode(),
                book.getAuthors().stream().map(author -> author.getName()).collect(Collectors.joining(" ")));
        String medium = text(book.getKeywords(),
                book.getCategories().stream().map(category -> category.getName()).collect(Collectors.joining(" ")));
        String weak = text(book.getDescription());
        int score = 0;
        for (String word : words) {
            score += (strong.contains(word) ? 5 : 0) + (medium.contains(word) ? 3 : 0) + (weak.contains(word) ? 1 : 0);
        }
        return score;
    }

    private static String text(String... values) {
        return Arrays.stream(values).filter(Objects::nonNull).collect(Collectors.joining(" ")).toLowerCase(Locale.ROOT);
    }

    /** Only letters and digits survive, so a question cannot add LIKE wildcards. */
    private static List<String> words(String question) {
        return Arrays.stream(question.toLowerCase(Locale.ROOT).split("[^\\p{L}\\p{M}\\p{N}]+"))
                .filter(word -> word.length() >= 3 && !COMMON_WORDS.contains(word))
                .distinct().limit(MAX_WORDS).toList();
    }

    private static String summary(CatalogBookView view, Book book) {
        String description = book == null || book.getDescription() == null ? ""
                : book.getDescription().replaceAll("\\s+", " ").strip();
        return "id=" + view.id() + " | " + view.title() + " | authors: " + String.join(", ", view.authors())
                + " | genres: " + String.join(", ", view.categories())
                + " | language: " + view.language() + " | year: " + (view.publishYear() == null ? "unknown" : view.publishYear())
                + " | copies on shelf: " + view.availableCopies() + " of " + view.totalCopies()
                + (view.canReserve() ? " (can be reserved)" : "")
                + " | " + (description.length() > DESCRIPTION ? description.substring(0, DESCRIPTION) + "…" : description);
    }
}
