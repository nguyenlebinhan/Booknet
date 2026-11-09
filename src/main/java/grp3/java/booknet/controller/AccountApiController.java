package grp3.java.booknet.controller;

import grp3.java.booknet.dto.AccountRequests;
import grp3.java.booknet.dto.ProfileResponse;
import grp3.java.booknet.security.AccountPrincipal;
import grp3.java.booknet.service.AccountService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/account")
@RequiredArgsConstructor
public class AccountApiController {
    private final AccountService accounts;

    @GetMapping("/profile")
    public ProfileResponse profile(Authentication authentication) {
        return accounts.profile(AccountPrincipal.id(authentication));
    }

    @PutMapping("/profile")
    public ProfileResponse updateProfile(Authentication authentication,
                                         @Valid @RequestBody AccountRequests.ProfileUpdate request) {
        return accounts.updateProfile(AccountPrincipal.id(authentication), request);
    }

    @PostMapping("/change-password")
    public Map<String, String> changePassword(Authentication authentication,
                                              @Valid @RequestBody AccountRequests.ChangePassword body,
                                              HttpServletRequest request, HttpServletResponse response) {
        accounts.changePassword(AccountPrincipal.id(authentication), body);
        new SecurityContextLogoutHandler().logout(request, response, authentication);
        jakarta.servlet.http.Cookie cookie = new jakarta.servlet.http.Cookie("remember-me", "");
        cookie.setPath(request.getContextPath().isEmpty() ? "/" : request.getContextPath());
        cookie.setHttpOnly(true);
        cookie.setSecure(request.isSecure());
        cookie.setMaxAge(0);
        response.addCookie(cookie);
        return AccountAuthApiController.message("Đã đổi mật khẩu. Vui lòng đăng nhập lại trên các thiết bị.");
    }
}
