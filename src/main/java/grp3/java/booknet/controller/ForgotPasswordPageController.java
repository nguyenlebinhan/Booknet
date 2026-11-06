package grp3.java.booknet.controller;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

/** Page route only: no email is sent yet. See docs/auth-pages-backend-handoff.md. */
@Controller
public class ForgotPasswordPageController {

    @GetMapping("/forgot-password")
    public String showForgotPasswordPage() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) {
            return "redirect:/";
        }
        return "forgot-password";
    }

    @PostMapping("/forgot-password")
    public String handleForgotPasswordSubmit() {
        return "redirect:/forgot-password?unavailable=true";
    }
}
