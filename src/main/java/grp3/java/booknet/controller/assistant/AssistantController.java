package grp3.java.booknet.controller.assistant;

import grp3.java.booknet.service.assistant.AssistantService;
import grp3.java.booknet.service.assistant.AssistantTurn;
import grp3.java.booknet.service.assistant.GuestLimitException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.List;

/**
 * AI book assistant (UC09) for guests and members. The chat bubble loads this panel; without
 * JavaScript the page works on its own. The conversation is kept only in the HTTP session.
 */
@Controller
@RequestMapping("/assistant")
@PreAuthorize("isAnonymous() or (hasAnyRole('USER', 'MEMBER') and !hasAnyRole('ADMIN', 'LIBRARIAN'))")
@RequiredArgsConstructor
public class AssistantController {
    private static final String CONVERSATION = "assistantConversation";
    private static final int MAX_TURNS = 20;

    private final AssistantService assistant;

    @GetMapping
    public String page(Model model, HttpServletRequest request, Authentication authentication) {
        return render(model, request, authentication);
    }

    @PostMapping
    public String send(@RequestParam(defaultValue = "") String message, Model model,
                       HttpServletRequest request, Authentication authentication) {
        String text = message.strip();
        if (text.isEmpty()) {
            model.addAttribute("messageError", "Type a message before sending.");
            return render(model, request, authentication);
        }
        if (text.length() > AssistantService.MAX_MESSAGE) {
            model.addAttribute("messageError", "Keep your message under " + AssistantService.MAX_MESSAGE + " characters.");
            model.addAttribute("draft", text.substring(0, AssistantService.MAX_MESSAGE));
            return render(model, request, authentication);
        }
        try {
            AssistantTurn turn = assistant.ask(text, conversation(request),
                    isGuest(authentication) ? request.getRemoteAddr() : null);
            List<AssistantTurn> turns = conversation(request);
            turns.add(turn);
            // Replace the stored list rather than change it, so a page being rendered never sees it change.
            request.getSession().setAttribute(CONVERSATION,
                    new ArrayList<>(turns.subList(Math.max(0, turns.size() - MAX_TURNS), turns.size())));
            return "redirect:/assistant";
        } catch (GuestLimitException exception) {
            model.addAttribute("messageError", "You have used today's " + assistant.guestLimit()
                    + " guest questions. Sign in to keep asking, or come back tomorrow.");
            model.addAttribute("draft", text);
            return render(model, request, authentication);
        }
    }

    @PostMapping(params = "reset")
    public String newConversation(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.removeAttribute(CONVERSATION);
        }
        return "redirect:/assistant";
    }

    private String render(Model model, HttpServletRequest request, Authentication authentication) {
        List<AssistantTurn> turns = conversation(request);
        boolean guest = isGuest(authentication);
        model.addAttribute("turns", turns);
        model.addAttribute("turnBooks", assistant.books(turns));
        model.addAttribute("connected", assistant.isConnected());
        model.addAttribute("guest", guest);
        if (guest) {
            model.addAttribute("guestLimit", assistant.guestLimit());
            model.addAttribute("guestRemaining", assistant.guestRemaining(request.getRemoteAddr()));
        }
        model.addAttribute("maxMessage", AssistantService.MAX_MESSAGE);
        model.addAttribute("activePage", "assistant");
        return "assistant/index";
    }

    @SuppressWarnings("unchecked")
    private static List<AssistantTurn> conversation(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        Object turns = session == null ? null : session.getAttribute(CONVERSATION);
        return turns == null ? new ArrayList<>() : new ArrayList<>((List<AssistantTurn>) turns);
    }

    private static boolean isGuest(Authentication authentication) {
        return authentication == null || authentication instanceof AnonymousAuthenticationToken;
    }
}
