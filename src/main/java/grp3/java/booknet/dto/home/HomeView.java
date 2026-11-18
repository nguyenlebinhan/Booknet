package grp3.java.booknet.dto.home;

import grp3.java.booknet.dto.catalog.CatalogBookView;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

/**
 * Real figures for the public home page (UC05). When the library has no published books yet,
 * {@link #hasBooks()} is false and the page keeps its sample shelf instead of showing nothing.
 */
public record HomeView(long publishedTitles, long members, List<Genre> genres, List<CatalogBookView> shelf,
                       List<QuickLink> popularSearches, OpeningStatus opening) {

    public boolean hasBooks() { return publishedTitles > 0 && !shelf.isEmpty(); }
    public String titlesText() { return format(publishedTitles); }
    public String membersText() { return format(members); }

    /** Numbers are written the Vietnamese way, as in the design: 12.486. */
    static String format(long value) {
        return NumberFormat.getIntegerInstance(Locale.forLanguageTag("vi-VN")).format(value);
    }

    public record Genre(Integer id, String name, long titles, String icon) {
        public String titlesText() { return format(titles) + (titles == 1 ? " title" : " titles"); }
    }

    /** A quick search under the hero: an author name (keyword) or a genre. */
    public record QuickLink(String label, String keyword, Integer categoryId) { }

    /** Whether the library is open now, and when it closes or next opens. */
    public record OpeningStatus(boolean open, String headline) { }
}
