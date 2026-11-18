package grp3.java.booknet.controller.account;

import grp3.java.booknet.dto.user.request.EmailRequest;
import grp3.java.booknet.dto.user.request.ResetPasswordRequest;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.CookieClearingLogoutHandler;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import grp3.java.booknet.dto.user.response.PasswordResetForm;
import grp3.java.booknet.enums.user.AccountTokenPurpose;
import grp3.java.booknet.exception.account.AccountException;
import grp3.java.booknet.service.impl.account.AccountServiceImpl;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class PasswordRecoveryController {
    private final AccountServiceImpl accounts;

    @GetMapping("/forgot-password")
    public String showForgotPasswordPage(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) {
            return "redirect:/";
        }
        if (!model.containsAttribute("requestSubmitted")) model.addAttribute("requestSubmitted", false);
        model.addAttribute("verificationRequest", false);
        return "auth/forgot-password";
    }

    @PostMapping("/forgot-password")
    public String handleForgotPasswordSubmit(
            @Valid @ModelAttribute("emailForm") EmailRequest form,
            BindingResult errors, Model model, RedirectAttributes redirect) {
        model.addAttribute("verificationRequest", false);
        model.addAttribute("requestSubmitted", false);
        model.addAttribute("submittedEmail", form.email());
        if (errors.hasErrors()) {
            model.addAttribute("emailError", "Please enter a valid email address of up to 120 characters.");
            return "auth/forgot-password";
        }
        accounts.requestEmail(form.email(), AccountTokenPurpose.RESET_PASSWORD);
        redirect.addFlashAttribute("requestSubmitted", true);
        redirect.addFlashAttribute("submittedEmail", form.email());
        return "redirect:/forgot-password";
    }

    @GetMapping("/reset-password")
    public String showResetPasswordPage(@RequestParam(required = false) String token,
                                         Model model, HttpServletResponse response) {
        response.setHeader("Referrer-Policy", "no-referrer");
        populateResetToken(token, model);
        return "auth/reset-password";
    }

    @PostMapping("/reset-password")
    public String resetPassword(@Valid @ModelAttribute("resetForm") PasswordResetForm form,
                                  BindingResult errors, Model model,
                                  HttpServletRequest request, HttpServletResponse response,
                                  Authentication authentication) {
        response.setHeader("Referrer-Policy", "no-referrer");
        if (!populateResetToken(form.getToken(), model)) return "auth/reset-password";
        if (form.getPassword() != null && !form.getPassword().equals(form.getConfirmPassword())) {
            errors.rejectValue("confirmPassword", "password.mismatch", "Passwords do not match.");
        }
        if (errors.hasErrors()) {
            if (errors.hasFieldErrors("password")) model.addAttribute("passwordError", errors.getFieldError("password").getDefaultMessage());
            if (errors.hasFieldErrors("confirmPassword")) model.addAttribute("confirmPasswordError", errors.getFieldError("confirmPassword").getDefaultMessage());
            return "auth/reset-password";
        }
        try {
            accounts.resetPassword(new ResetPasswordRequest(form.getToken(), form.getPassword(), form.getConfirmPassword()));
        } catch (AccountException ex) {
            if (populateResetToken(form.getToken(), model)) model.addAttribute("passwordError", ex.getMessage());
            return "auth/reset-password";
        }
        new CookieClearingLogoutHandler("JSESSIONID", "remember-me").logout(request, response, authentication);
        new SecurityContextLogoutHandler().logout(request, response, authentication);
        return "redirect:/login?reset=true";
    }

    private boolean populateResetToken(String token, Model model) {
        model.addAttribute("invitation",accounts.isInvitation(token));
        try {
            accounts.validateResetToken(token);
            model.addAttribute("tokenValid", true);
            model.addAttribute("token", token);
            return true;
        } catch (AccountException ex) {
            model.addAttribute("tokenValid", false);
            return false;
        }
    }
}
