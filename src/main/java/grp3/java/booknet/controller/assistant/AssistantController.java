package grp3.java.booknet.controller.assistant;

import grp3.java.booknet.service.assistant.AssistantService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * AI book assistant page (UC09), UI only. The page works without JavaScript: the form posts the
 * message and the page shows it with the assistant's reply. Nothing is stored between requests.
 */
@Controller
@RequestMapping("/assistant")
@RequiredArgsConstructor
public class AssistantController {
    private final AssistantService assistant;

    @GetMapping
    public String page(Model model) {
        return render(model, null, null);
    }

    @PostMapping
    public String send(@RequestParam(defaultValue = "") String message, Model model) {
        String text = message.strip();
        if (text.isEmpty()) {
            model.addAttribute("messageError", "Type a message before sending.");
            return render(model, null, null);
        }
        if (text.length() > AssistantService.MAX_MESSAGE) {
            model.addAttribute("messageError", "Keep your message under " + AssistantService.MAX_MESSAGE + " characters.");
            model.addAttribute("draft", text.substring(0, AssistantService.MAX_MESSAGE));
            return render(model, null, null);
        }
        return render(model, text, assistant.reply(text));
    }

    private String render(Model model, String sent, String reply) {
        model.addAttribute("sampleBooks", assistant.sampleBooks());
        model.addAttribute("sent", sent);
        model.addAttribute("reply", reply);
        model.addAttribute("maxMessage", AssistantService.MAX_MESSAGE);
        // The script shows the same reply instantly instead of reloading the page.
        model.addAttribute("cannedReply", assistant.reply(""));
        model.addAttribute("activePage", "assistant");
        return "assistant/index";
    }
}
