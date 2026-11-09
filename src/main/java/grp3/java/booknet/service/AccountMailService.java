package grp3.java.booknet.service;

import grp3.java.booknet.enums.user.AccountTokenPurpose;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@RequiredArgsConstructor
public class AccountMailService {
    private final JavaMailSender mailSender;

    @Value("${app.account.base-url:http://localhost:8080}")
    private String baseUrl;
    @Value("${app.account.mail-from:no-reply@booknet.local}")
    private String from;

    public void send(String email, String token, AccountTokenPurpose purpose) {
        boolean verification = purpose == AccountTokenPurpose.VERIFY_EMAIL;
        String url = UriComponentsBuilder.fromUriString(baseUrl)
                .path(verification ? "/api/auth/verify-email" : "/api/auth/reset-password")
                .queryParam("token", token).build().encode().toUriString();
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(email);
        message.setSubject(verification ? "BookNet - Xác thực email" : "BookNet - Đặt lại mật khẩu");
        message.setText(verification
                ? "Xác thực email BookNet bằng liên kết sau (hiệu lực 24 giờ, dùng một lần):\n" + url
                : "Yêu cầu đặt lại mật khẩu BookNet (hiệu lực 30 phút, dùng một lần):\n" + url
                    + "\nMã đặt lại mật khẩu: " + token
                    + "\nNếu bạn không yêu cầu, hãy bỏ qua email này.");
        mailSender.send(message);
    }
}
