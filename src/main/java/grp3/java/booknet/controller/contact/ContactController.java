package grp3.java.booknet.controller.contact;

import grp3.java.booknet.dto.contact.ContactForm;
import grp3.java.booknet.service.contact.ContactMailService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Slf4j
@Controller
@RequestMapping("/contact")
@RequiredArgsConstructor
public class ContactController {
    private final ContactMailService mail;

    @InitBinder("contactForm")
    void bindForm(WebDataBinder binder) {
        binder.setDeclarativeBinding(true);
    }

    @ModelAttribute("activePage")
    String activePage() {
        // Matches the navbar's highlight check; this is not a view name.
        return "contact";
    }

    @GetMapping
    public String contact(Model model) {
        model.addAttribute("contactForm", new ContactForm("", "", "", "Borrowing & renewals", ""));
        return "pages/contact";
    }

    @PostMapping
    public String submit(@Valid @ModelAttribute("contactForm") ContactForm form,
                         BindingResult errors, RedirectAttributes redirect) {
        if (errors.hasErrors()) return "pages/contact";
        try {
            mail.send(form);
        } catch (MailException ex) {
            log.warn("Contact email delivery failed; check SMTP and recipient configuration ({})",
                    ex.getClass().getSimpleName());
            errors.reject("contact.delivery", "Unable to send your message right now. Please try again later.");
            return "pages/contact";
        }
        redirect.addFlashAttribute("contactSuccess",
                "Your message has been submitted to the BookNet librarians. We will reply to your email as soon as possible.");
        return "redirect:/contact";
    }
}
