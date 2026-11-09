package grp3.java.booknet.controller;

import grp3.java.booknet.dto.AccountRequests;
import grp3.java.booknet.enums.user.AccountTokenPurpose;
import grp3.java.booknet.exception.AccountException;
import grp3.java.booknet.service.AccountService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class EmailVerificationController {
    private final AccountService accounts;

    @GetMapping("/verify-email")
    public String verifyEmail(@RequestParam(required = false) String token,
                               RedirectAttributes redirect, HttpServletResponse response) {
        response.setHeader("Referrer-Policy", "no-referrer");
        try {
            accounts.verifyEmail(token);
            redirect.addFlashAttribute("successMessage", "Xác thực email thành công. Bạn có thể đăng nhập.");
        } catch (AccountException ex) {
            redirect.addFlashAttribute("errorMessage", "Liên kết xác thực không hợp lệ, hết hạn hoặc đã dùng. Vui lòng yêu cầu email xác thực mới.");
        }
        return "redirect:/login";
    }

    @GetMapping("/verification/resend")
    public String showResendPage(Model model) {
        model.addAttribute("verificationRequest", true);
        if (!model.containsAttribute("requestSubmitted")) model.addAttribute("requestSubmitted", false);
        return "forgot-password";
    }

    @PostMapping("/verification/resend")
    public String resend(@Valid @ModelAttribute("emailForm") AccountRequests.EmailRequest form,
                          BindingResult errors, Model model, RedirectAttributes redirect) {
        model.addAttribute("verificationRequest", true);
        model.addAttribute("requestSubmitted", false);
        model.addAttribute("submittedEmail", form.email());
        if (errors.hasErrors()) {
            model.addAttribute("emailError", "Vui lòng nhập email hợp lệ, tối đa 120 ký tự.");
            return "forgot-password";
        }
        accounts.requestEmail(form.email(), AccountTokenPurpose.VERIFY_EMAIL);
        redirect.addFlashAttribute("requestSubmitted", true);
        redirect.addFlashAttribute("submittedEmail", form.email());
        return "redirect:/verification/resend";
    }
}
