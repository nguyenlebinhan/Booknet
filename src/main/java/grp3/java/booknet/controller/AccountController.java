package grp3.java.booknet.controller;

import grp3.java.booknet.dto.AccountRequests;
import grp3.java.booknet.dto.ProfileResponse;
import grp3.java.booknet.exception.AccountException;
import grp3.java.booknet.security.AccountPrincipal;
import grp3.java.booknet.service.AccountService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.CookieClearingLogoutHandler;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/account")
@RequiredArgsConstructor
public class AccountController {
    private final AccountService accounts;

    @InitBinder({"profileForm", "passwordForm"})
    void bindForms(WebDataBinder binder) {
        // These records bind only their constructor fields, never entity properties.
        binder.setDeclarativeBinding(true);
    }

    @GetMapping("/profile")
    public String profile(Authentication authentication, Model model) {
        ProfileResponse profile = accounts.profile(AccountPrincipal.id(authentication));
        model.addAttribute("profile", profile);
        model.addAttribute("profileForm", new AccountRequests.ProfileUpdate(
                profile.fullName(), profile.phone(), profile.address(), profile.avatarUrl()));
        return "profile";
    }

    @PostMapping("/profile")
    public String updateProfile(Authentication authentication,
                                @Valid @ModelAttribute("profileForm") AccountRequests.ProfileUpdate form,
                                BindingResult errors, Model model, RedirectAttributes redirect) {
        Long userId = AccountPrincipal.id(authentication);
        if (!errors.hasErrors()) {
            try {
                accounts.updateProfile(userId, form);
                redirect.addFlashAttribute("successMessage", "Your profile has been updated.");
                return "redirect:/account/profile";
            } catch (AccountException ex) {
                errors.reject("profile.invalid", ex.getMessage());
            }
        }
        model.addAttribute("profile", accounts.profile(userId));
        return "profile";
    }

    @GetMapping("/change-password")
    public String showChangePassword(Authentication authentication, Model model) {
        model.addAttribute("profile", accounts.profile(AccountPrincipal.id(authentication)));
        model.addAttribute("passwordForm", new AccountRequests.ChangePassword(null, null, null));
        return "change-password";
    }

    @PostMapping("/change-password")
    public String changePassword(Authentication authentication,
                                 @Valid @ModelAttribute("passwordForm") AccountRequests.ChangePassword form,
                                 BindingResult errors, Model model, RedirectAttributes redirect,
                                 HttpServletRequest request, HttpServletResponse response) {
        Long userId = AccountPrincipal.id(authentication);
        if (!errors.hasErrors()) {
            try {
                accounts.changePassword(userId, form);
                new CookieClearingLogoutHandler("JSESSIONID", "remember-me").logout(request, response, authentication);
                new SecurityContextLogoutHandler().logout(request, response, authentication);
                redirect.addFlashAttribute("successMessage", "Your password has been changed. Please sign in with your new password.");
                return "redirect:/login";
            } catch (AccountException ex) {
                errors.reject("password.invalid", ex.getMessage());
            }
        }
        model.addAttribute("profile", accounts.profile(userId));
        return "change-password";
    }
}
