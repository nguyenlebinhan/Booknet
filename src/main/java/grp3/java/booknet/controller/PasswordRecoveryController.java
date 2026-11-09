package grp3.java.booknet.controller;

import grp3.java.booknet.dto.AccountRequests;
import grp3.java.booknet.dto.PasswordResetForm;
import grp3.java.booknet.enums.user.AccountTokenPurpose;
import grp3.java.booknet.exception.AccountException;
import grp3.java.booknet.service.AccountService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.security.web.authentication.logout.CookieClearingLogoutHandler;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;

@Controller
@RequiredArgsConstructor
public class PasswordRecoveryController {

    private final AccountService accounts;

    @GetMapping("/forgot-password")
    public String showForgotPasswordPage(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) {
            return "redirect:/";
        }
        if (!model.containsAttribute("requestSubmitted")) model.addAttribute("requestSubmitted", false);
        model.addAttribute("verificationRequest", false);
        return "forgot-password";
    }

    @PostMapping("/forgot-password")
    public String handleForgotPasswordSubmit(
            @Valid @ModelAttribute("emailForm") AccountRequests.EmailRequest form,
            BindingResult errors, Model model, RedirectAttributes redirect) {
        model.addAttribute("verificationRequest", false);
        model.addAttribute("requestSubmitted", false);
        model.addAttribute("submittedEmail", form.email());
        if (errors.hasErrors()) {
            model.addAttribute("emailError", "Vui lòng nhập email hợp lệ, tối đa 120 ký tự.");
            return "forgot-password";
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
        return "reset-password";
    }

    @PostMapping("/reset-password")
    public String resetPassword(@Valid @ModelAttribute("resetForm") PasswordResetForm form,
                                  BindingResult errors, Model model,
                                  HttpServletRequest request, HttpServletResponse response,
                                  Authentication authentication) {
        response.setHeader("Referrer-Policy", "no-referrer");
        if (!populateResetToken(form.getToken(), model)) return "reset-password";
        if (form.getPassword() != null && !form.getPassword().equals(form.getConfirmPassword())) {
            errors.rejectValue("confirmPassword", "password.mismatch", "Mật khẩu xác nhận không trùng khớp.");
        }
        if (errors.hasErrors()) {
            if (errors.hasFieldErrors("password")) model.addAttribute("passwordError", errors.getFieldError("password").getDefaultMessage());
            if (errors.hasFieldErrors("confirmPassword")) model.addAttribute("confirmPasswordError", errors.getFieldError("confirmPassword").getDefaultMessage());
            return "reset-password";
        }
        try {
            accounts.resetPassword(new AccountRequests.ResetPassword(form.getToken(), form.getPassword(), form.getConfirmPassword()));
        } catch (AccountException ex) {
            if (populateResetToken(form.getToken(), model)) model.addAttribute("passwordError", ex.getMessage());
            return "reset-password";
        }
        new CookieClearingLogoutHandler("JSESSIONID", "remember-me").logout(request, response, authentication);
        new SecurityContextLogoutHandler().logout(request, response, authentication);
        return "redirect:/login?reset=true";
    }

    private boolean populateResetToken(String token, Model model) {
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
