package grp3.java.booknet.service.assistant;

import grp3.java.booknet.dto.catalog.CatalogBookView;
import grp3.java.booknet.dto.catalog.CatalogFilter;
import grp3.java.booknet.service.catalog.CatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * The AI book assistant (UC09) — UI only for now. The team has not chosen an AI service, so
 * replies are fixed text and nothing is sent anywhere or stored. When a provider is chosen,
 * {@link #reply} is the one method to replace; the page and its form stay the same.
 */
@Service
@RequiredArgsConstructor
public class AssistantService {
    public static final int MAX_MESSAGE = 1000;

    private final CatalogService catalog;

    /** Two real books for the sample conversation, most borrowed first. Empty when the catalog is empty. */
    public List<CatalogBookView> sampleBooks() {
        CatalogFilter popular = new CatalogFilter();
        popular.setSort("popular");
        return catalog.search(popular, PageRequest.of(0, 2), false).getContent();
    }

    /** The answer shown for a member's message. Fixed text until an AI service is connected. */
    public String reply(String message) {
        return "The AI assistant is not connected yet, so it cannot answer this here. "
                + "You can search the catalog for what you asked, or ask a librarian, who will be glad to help.";
    }
}
