package grp3.java.booknet.service.home;

import grp3.java.booknet.dto.catalog.CatalogBookView;
import grp3.java.booknet.dto.catalog.CatalogFilter;
import grp3.java.booknet.dto.home.HomeView;
import grp3.java.booknet.dto.home.HomeView.Genre;
import grp3.java.booknet.dto.home.HomeView.OpeningStatus;
import grp3.java.booknet.dto.home.HomeView.QuickLink;
import grp3.java.booknet.enums.book.PublicationStatus;
import grp3.java.booknet.enums.user.UserRole;
import grp3.java.booknet.repository.book.BookRepository;
import grp3.java.booknet.repository.catalog.CatalogStatsRepository;
import grp3.java.booknet.repository.user.UserRepository;
import grp3.java.booknet.service.catalog.CatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/** Figures for the public home page (UC05), taken from the catalog and member accounts. Read-only. */
@Service
@RequiredArgsConstructor
public class HomeService {
    static final ZoneId LIBRARY_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final List<String> GENRE_ICONS = List.of("book-open", "sprout", "globe", "sparkles");

    private final BookRepository books;
    private final UserRepository users;
    private final CatalogStatsRepository stats;
    private final CatalogService catalog;
    private final Clock clock;

    @Transactional(readOnly = true)
    public HomeView home() {
        long titles = books.countByDeletedFalseAndPublicationStatus(PublicationStatus.PUBLISHED);
        List<Genre> genres = new ArrayList<>();
        List<Object[]> rows = stats.findLargestGenres(PublicationStatus.PUBLISHED, PageRequest.of(0, GENRE_ICONS.size()));
        for (int i = 0; i < rows.size(); i++) {
            Object[] row = rows.get(i);
            genres.add(new Genre((Integer) row[0], (String) row[1], ((Number) row[2]).longValue(), GENRE_ICONS.get(i)));
        }

        CatalogFilter mostPopular = new CatalogFilter();
        mostPopular.setSort("popular");
        List<CatalogBookView> shelf = titles == 0 ? List.of()
                : catalog.search(mostPopular, PageRequest.of(0, 4), false).getContent();

        List<QuickLink> quick = new ArrayList<>();
        shelf.stream().flatMap(book -> book.authors().stream()).findFirst()
                .ifPresent(author -> quick.add(new QuickLink(author, author, null)));
        genres.stream().limit(2).forEach(genre -> quick.add(new QuickLink(genre.name(), null, genre.id())));

        return new HomeView(titles, users.countByRole(UserRole.ROLE_USER), genres, shelf, quick,
                opening(LocalDateTime.now(clock.withZone(LIBRARY_ZONE))));
    }

    /**
     * Opening hours as printed in the site footer: Monday to Saturday 08:00–20:00, Sunday 09:00–17:00.
     */
    static OpeningStatus opening(LocalDateTime now) {
        LocalTime opens = opensAt(now.getDayOfWeek());
        LocalTime closes = closesAt(now.getDayOfWeek());
        LocalTime time = now.toLocalTime();
        if (!time.isBefore(opens) && time.isBefore(closes)) {
            return new OpeningStatus(true, "booknet welcomes you until " + closes + " today");
        }
        if (time.isBefore(opens)) {
            return new OpeningStatus(false, "booknet opens at " + opens + " today");
        }
        return new OpeningStatus(false, "booknet opens at " + opensAt(now.getDayOfWeek().plus(1)) + " tomorrow");
    }

    private static LocalTime opensAt(DayOfWeek day) {
        return day == DayOfWeek.SUNDAY ? LocalTime.of(9, 0) : LocalTime.of(8, 0);
    }

    private static LocalTime closesAt(DayOfWeek day) {
        return day == DayOfWeek.SUNDAY ? LocalTime.of(17, 0) : LocalTime.of(20, 0);
    }
}
