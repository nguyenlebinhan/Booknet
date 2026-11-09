package grp3.java.booknet.controller;

import grp3.java.booknet.dto.UserRegisterDto;
import grp3.java.booknet.service.UserService;
import grp3.java.booknet.exception.AccountException;
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
            model.addAttribute("errorMessage", "Không thể đăng nhập. Kiểm tra email, mật khẩu và xác thực email; tài khoản phải đang hoạt động.");
        }
        if (logout != null) {
            model.addAttribute("logoutMessage", "Bạn đã đăng xuất thành công khỏi hệ thống.");
        }
        if (registered != null) {
            model.addAttribute("successMessage", "Đăng ký thành công! Vui lòng kiểm tra email và xác thực tài khoản trước khi đăng nhập.");
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
            redirectAttributes.addFlashAttribute("successMessage", "Đăng ký thành công! Vui lòng kiểm tra email để xác thực tài khoản.");
            return "redirect:/login?registered=true";
        } catch (AccountException e) {
            model.addAttribute("globalError", e.getMessage());
            return "register";
        } catch (Exception e) {
            log.error("Lỗi trong quá trình đăng ký tài khoản: ", e);
            model.addAttribute("globalError", "Chưa thể đăng ký tài khoản. Vui lòng thử lại sau.");
            return "register";
        }
    }
}
