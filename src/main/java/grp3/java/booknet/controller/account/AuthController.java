package grp3.java.booknet.controller.account;

import grp3.java.booknet.dto.user.response.UserRegisterDto;
import grp3.java.booknet.service.account.UserService;
import grp3.java.booknet.security.navigation.LoginDestination;
import grp3.java.booknet.exception.account.AccountException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Slf4j
@Controller
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @GetMapping("/login")
    public String showLoginPage(
            @RequestParam(value = "error", required = false) String error,
            @RequestParam(value = "logout", required = false) String logout,
            @RequestParam(value = "registered", required = false) String registered,
            @RequestParam(value = "reset", required = false) String reset,
            @RequestParam(value = "oauth2_error", required = false) String oauth2Error,
            Model model) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) {
            return "redirect:" + LoginDestination.forAuthentication(auth);
        }

        if (error != null) {
            model.addAttribute("errorMessage", "Unable to sign in. Check your email and password, verify your email address, and make sure your account is active.");
        }
        if (logout != null) {
            model.addAttribute("logoutMessage", "You have signed out successfully.");
        }
        if (registered != null) {
            model.addAttribute("successMessage", "Registration successful! Please check your email and verify your account before signing in.");
        }
        if (reset != null) {
            model.addAttribute("successMessage", "Your password has been reset. Please sign in with your new password.");
        }
        if (oauth2Error != null) {
            model.addAttribute("errorMessage", "Google sign-in failed or was canceled. If you already have a BookNet account with this email, sign in with your email and password, then link Google from the home page.");
        }

        return "auth/login";
    }

    @GetMapping("/register")
    public String showRegisterPage(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) {
            return "redirect:" + LoginDestination.forAuthentication(auth);
        }

        if (!model.containsAttribute("user")) {
            model.addAttribute("user", new UserRegisterDto());
        }
        return "auth/register";
    }

    @PostMapping("/register")
    public String processRegister(
            @Valid @ModelAttribute("user") UserRegisterDto registerDto,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (registerDto.getPassword() != null && !registerDto.getPassword().equals(registerDto.getConfirmPassword())) {
            bindingResult.rejectValue("confirmPassword", "password.mismatch", "Passwords do not match.");
        }

        if (registerDto.getEmail() != null && userService.existsByEmail(registerDto.getEmail())) {
            bindingResult.rejectValue("email", "email.duplicate", "This email address is already registered.");
        }

        if (bindingResult.hasErrors()) {
            return "auth/register";
        }

        try {
            userService.registerNewUser(registerDto);
            redirectAttributes.addFlashAttribute("successMessage", "Registration successful! Please check your email to verify your account.");
            return "redirect:/login?registered=true";
        } catch (AccountException e) {
            model.addAttribute("globalError", e.getMessage());
            return "auth/register";
        } catch (Exception e) {
            log.error("Account registration failed: ", e);
            model.addAttribute("globalError", "Unable to register your account right now. Please try again later.");
            return "auth/register";
        }
    }
}
