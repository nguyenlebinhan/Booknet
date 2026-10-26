package grp3.java.booknet.controller;

import grp3.java.booknet.dto.UserLoginDto;
import grp3.java.booknet.dto.UserRegisterDto;
import grp3.java.booknet.dto.UserSessionDto;
import grp3.java.booknet.service.AuthService;
import grp3.java.booknet.service.BookCatalogService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final AuthService authService;
    private final BookCatalogService bookCatalogService;

    @GetMapping("/")
    public String home(Model model, HttpSession session) {
        // Danh mục sách truyền từ backend xuống Thymeleaf
        model.addAttribute("books", bookCatalogService.getFeaturedBooks());

        if (!model.containsAttribute("loginDto")) {
            model.addAttribute("loginDto", new UserLoginDto());
        }
        if (!model.containsAttribute("registerDto")) {
            model.addAttribute("registerDto", new UserRegisterDto());
        }

        // Lấy thông tin phiên đăng nhập nếu có
        UserSessionDto currentUser = (UserSessionDto) session.getAttribute("currentUser");
        model.addAttribute("currentUser", currentUser);

        return "index";
    }

    /**
     * Xử lý đăng ký tài khoản hoàn toàn tại Backend Spring MVC
     * Requirement: Sign up -> You registered successfully!
     */
    @PostMapping("/register")
    public String handleRegister(@ModelAttribute("registerDto") UserRegisterDto registerDto,
                                 RedirectAttributes redirectAttributes) {
        try {
            authService.register(registerDto);

            // Backend thông báo thành công và chuyển tab sang login
            redirectAttributes.addFlashAttribute("alertMessage", "You registered successfully!");
            redirectAttributes.addFlashAttribute("successMessage", "You registered successfully!");
            redirectAttributes.addFlashAttribute("activeTab", "login");
            redirectAttributes.addFlashAttribute("prefillEmail", registerDto.getEmail());

        } catch (IllegalArgumentException ex) {
            // Backend thông báo lỗi và giữ tab đăng ký
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            redirectAttributes.addFlashAttribute("activeTab", "signup");
            redirectAttributes.addFlashAttribute("registerDto", registerDto);
        }

        return "redirect:/";
    }

    /**
     * Xử lý đăng nhập hoàn toàn tại Backend Spring MVC
     * Requirement: Login -> Welcome to home page!
     */
    @PostMapping("/login")
    public String handleLogin(@ModelAttribute("loginDto") UserLoginDto loginDto,
                              HttpSession session,
                              RedirectAttributes redirectAttributes) {
        try {
            UserSessionDto userSession = authService.login(loginDto);

            // Lưu người dùng đăng nhập vào HttpSession
            session.setAttribute("currentUser", userSession);

            // Backend gửi thông báo đăng nhập thành công
            redirectAttributes.addFlashAttribute("alertMessage", "Welcome to home page!");
            redirectAttributes.addFlashAttribute("successMessage", "Welcome to home page!");

        } catch (IllegalArgumentException ex) {
            // Backend gửi thông báo lỗi đăng nhập
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            redirectAttributes.addFlashAttribute("activeTab", "login");
            redirectAttributes.addFlashAttribute("loginDto", loginDto);
        }

        return "redirect:/";
    }

    /**
     * Xử lý đăng xuất tại Backend Spring MVC
     */
    @GetMapping("/logout")
    public String handleLogout(HttpSession session, RedirectAttributes redirectAttributes) {
        session.removeAttribute("currentUser");
        session.invalidate();
        redirectAttributes.addFlashAttribute("successMessage", "Bạn đã đăng xuất khỏi hệ thống thành công.");
        return "redirect:/";
    }
}
