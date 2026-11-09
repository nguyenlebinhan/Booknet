package grp3.java.booknet.controller;

import grp3.java.booknet.dto.AccountRequests;
import grp3.java.booknet.dto.UserRegisterDto;
import grp3.java.booknet.enums.user.AccountTokenPurpose;
import grp3.java.booknet.service.AccountService;
import grp3.java.booknet.service.UserService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Validated
public class AccountAuthApiController {
    private final AccountService accounts;
    private final UserService users;

    @GetMapping("/csrf")
    public Map<String, String> csrf(CsrfToken token) {
        return Map.of("headerName", token.getHeaderName(), "parameterName", token.getParameterName(), "token", token.getToken());
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, String> register(@Valid @RequestBody UserRegisterDto request) {
        users.registerNewUser(request);
        return message("Đăng ký thành công. Vui lòng kiểm tra email để xác thực tài khoản trước khi đăng nhập.");
    }

    @PostMapping("/forgot-password")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Map<String, String> forgotPassword(@Valid @RequestBody AccountRequests.EmailRequest request) {
        accounts.requestEmail(request.email(), AccountTokenPurpose.RESET_PASSWORD);
        return message("Nếu tài khoản đủ điều kiện, hướng dẫn đặt lại mật khẩu sẽ được gửi qua email.");
    }

    @PostMapping("/verification/resend")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Map<String, String> resendVerification(@Valid @RequestBody AccountRequests.EmailRequest request) {
        accounts.requestEmail(request.email(), AccountTokenPurpose.VERIFY_EMAIL);
        return message("Nếu tài khoản cần xác thực, hướng dẫn sẽ được gửi qua email.");
    }

    @GetMapping("/verify-email")
    public Map<String, String> verifyEmail(@RequestParam @NotBlank @Size(max = 200) String token,
                                          HttpServletResponse response) {
        response.setHeader("Referrer-Policy", "no-referrer");
        accounts.verifyEmail(token);
        return message("Xác thực email thành công. Bạn có thể đăng nhập.");
    }

    @GetMapping("/reset-password")
    public Map<String, String> checkResetToken(@RequestParam @NotBlank @Size(max = 200) String token,
                                               HttpServletResponse response) {
        response.setHeader("Referrer-Policy", "no-referrer");
        accounts.validateResetToken(token);
        return message("Liên kết hợp lệ. Gửi token, newPassword và confirmPassword bằng POST /api/auth/reset-password để đặt lại mật khẩu.");
    }

    @PostMapping("/reset-password")
    public Map<String, String> resetPassword(@Valid @RequestBody AccountRequests.ResetPassword request) {
        accounts.resetPassword(request);
        return message("Đã đặt lại mật khẩu. Vui lòng đăng nhập bằng mật khẩu mới.");
    }

    static Map<String, String> message(String message) {
        return Map.of("message", message);
    }
}
