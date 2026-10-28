package grp3.java.booknet.controller;

import grp3.java.booknet.dto.UserRegisterDto;
import grp3.java.booknet.service.UserService;
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
            @RequestParam(value = "oauth2_error", required = false) String oauth2Error,
            Model model) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) {
            return "redirect:/";
        }

        if (error != null) {
            model.addAttribute("errorMessage", "Email hoặc mật khẩu không chính xác. Vui lòng thử lại!");
        }
        if (logout != null) {
            model.addAttribute("logoutMessage", "Bạn đã đăng xuất thành công khỏi hệ thống.");
        }
        if (registered != null) {
            model.addAttribute("successMessage", "Đăng ký tài khoản thành công! Hãy đăng nhập để trải nghiệm.");
        }
        if (oauth2Error != null) {
            model.addAttribute("errorMessage", "Đăng nhập Google thất bại hoặc bị hủy. Nếu email đã có tài khoản BookNet, hãy đăng nhập bằng email và mật khẩu rồi liên kết Google từ trang Home.");
        }

        return "login";
    }

    @GetMapping("/register")
    public String showRegisterPage(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) {
            return "redirect:/";
        }

        if (!model.containsAttribute("user")) {
            model.addAttribute("user", new UserRegisterDto());
        }
        return "register";
    }

    @PostMapping("/register")
    public String processRegister(
            @Valid @ModelAttribute("user") UserRegisterDto registerDto,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (registerDto.getPassword() != null && !registerDto.getPassword().equals(registerDto.getConfirmPassword())) {
            bindingResult.rejectValue("confirmPassword", "password.mismatch", "Mật khẩu xác nhận không trùng khớp!");
        }

        if (registerDto.getEmail() != null && userService.existsByEmail(registerDto.getEmail())) {
            bindingResult.rejectValue("email", "email.duplicate", "Địa chỉ email này đã được sử dụng!");
        }

        if (bindingResult.hasErrors()) {
            return "register";
        }

        try {
            userService.registerNewUser(registerDto);
            redirectAttributes.addFlashAttribute("successMessage", "Đăng ký thành công! Vui lòng đăng nhập.");
            return "redirect:/login?registered=true";
        } catch (Exception e) {
            log.error("Lỗi trong quá trình đăng ký tài khoản: ", e);
            model.addAttribute("globalError", "Đã xảy ra lỗi trong quá trình đăng ký: " + e.getMessage());
            return "register";
        }
    }
}
