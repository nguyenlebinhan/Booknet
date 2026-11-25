package grp3.java.booknet.service.assistant;

import grp3.java.booknet.dto.catalog.CatalogBookView;
import grp3.java.booknet.service.admin.CirculationPolicyService;
import grp3.java.booknet.service.catalog.CatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The AI book assistant (UC09). Gemini answers only about Booknet: its catalog, rules and website.
 * Account data is never sent. Books shown come from the database, and only from the list sent with
 * the question, so an invented book cannot appear as a card.
 */
@Service
@RequiredArgsConstructor
public class AssistantService {
    public static final int MAX_MESSAGE = 1000;
    public static final String OFF_TOPIC = "I can only help with Booknet: finding books in our catalog, borrowing, "
            + "reservations, renewals, fines, opening hours and using this website. What would you like to know about the library?";
    public static final String UNAVAILABLE = "The AI assistant cannot answer right now. "
            + "You can search the catalog for what you asked, or ask a librarian, who will be glad to help.";

    /** Earlier answers sent along so follow-up questions make sense. */
    private static final int CONTEXT_TURNS = 4;
    private static final int MAX_CARDS = 4;
    private static final Pattern EMAIL = Pattern.compile("[A-Za-z0-9_.+-]+@[A-Za-z0-9_-]+([.][A-Za-z0-9_-]+)+");
    private static final Pattern PHONE = Pattern.compile("(?<![0-9-])(?:[+]84|0)(?:[ .-]?[0-9]){9,10}(?![0-9])");

    private final CatalogService catalog;
    private final AssistantBookFinder finder;
    private final GeminiClient gemini;
    private final GuestQuota guestQuota;
    private final CirculationPolicyService policyService;

    /** Current card data for the books in a conversation, by ID. Books no longer published are left out. */
    public Map<Long, CatalogBookView> books(Collection<AssistantTurn> turns) {
        Set<Long> ids = turns.stream().flatMap(turn -> turn.bookIds().stream()).collect(Collectors.toSet());
        return catalog.findPublished(ids).stream().collect(Collectors.toMap(CatalogBookView::id, Function.identity()));
    }

    public boolean isConnected() {
        return gemini.isConfigured();
    }

    public int guestLimit() {
        return guestQuota.limit();
    }

    public int guestRemaining(String address) {
        return guestQuota.remaining(address);
    }

    /**
     * Answers one question.
     *
     * @param guestAddress the guest's IP address, or null for a signed-in reader (no limit)
     * @throws GuestLimitException when the guest has used today's questions
     */
    public AssistantTurn ask(String message, List<AssistantTurn> history, String guestAddress) {
        boolean limited = guestAddress != null && gemini.isConfigured();
        if (limited && !guestQuota.tryUse(guestAddress)) {
            throw new GuestLimitException();
        }
        boolean answered = false;
        try {
            String question = withoutContactDetails(message);
            List<AssistantTurn> context = history.stream().filter(AssistantTurn::answered).toList();
            context = context.subList(Math.max(0, context.size() - CONTEXT_TURNS), context.size());
            List<AssistantBookFinder.Candidate> candidates = finder.find(question,
                    context.stream().flatMap(turn -> turn.bookIds().stream()).distinct().toList());
            try {
                List<String> genres = catalog.getCategories().stream().map(category -> category.getName()).toList();
                GeminiClient.Answer answer = gemini.ask(AssistantPrompt.build(genres, candidates, policyService.getPolicy()), context, question);
                answered = true;
                if (!answer.onTopic()) {
                    // Fixed text: whatever the model wrote about another topic is never shown.
                    return new AssistantTurn(question, OFF_TOPIC, List.of(), false, false);
                }
                Set<Long> offered = candidates.stream().map(AssistantBookFinder.Candidate::id).collect(Collectors.toSet());
                return new AssistantTurn(question, answer.text(),
                        cards(answer.bookIds().stream().filter(offered::contains)), true, false);
            } catch (AssistantUnavailableException exception) {
                return new AssistantTurn(question, UNAVAILABLE, cards(candidates.stream()
                        .filter(AssistantBookFinder.Candidate::matched).map(AssistantBookFinder.Candidate::id)), false, true);
            }
        } finally {
            if (limited && !answered) {
                guestQuota.giveBack(guestAddress);
            }
        }
    }

    private static List<Long> cards(Stream<Long> ids) {
        return ids.distinct().limit(MAX_CARDS).collect(Collectors.toCollection(ArrayList::new));
    }

    /** Emails and Vietnamese phone numbers are removed before a question leaves the server. */
    static String withoutContactDetails(String message) {
        return PHONE.matcher(EMAIL.matcher(message).replaceAll("[email removed]")).replaceAll("[phone removed]");
    }
}
