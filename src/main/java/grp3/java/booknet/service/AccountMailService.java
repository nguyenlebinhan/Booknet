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
                .path(verification ? "/verify-email" : "/reset-password")
                .queryParam("token", token).build().encode().toUriString();
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(email);
        message.setSubject(verification ? "BookNet - Verify your email" : "BookNet - Reset your password");
        message.setText(verification
                ? "Verify your BookNet email address using this link (valid for 24 hours, one use only):\n" + url
                : "Reset your BookNet password using this link (valid for 15 minutes, one use only):\n" + url
                    + "\nPassword reset code: " + token
                    + "\nIf you did not request this, please ignore this email.");
        mailSender.send(message);
    }
}
