package grp3.java.booknet.service.impl.account;

import grp3.java.booknet.enums.user.AccountTokenPurpose;
import grp3.java.booknet.service.account.AccountMailService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import grp3.java.booknet.service.mail.BooknetMailService;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@RequiredArgsConstructor
public class AccountMailServiceImpl implements AccountMailService {
    private final BooknetMailService mailSender;

    @Value("${app.account.base-url:http://localhost:8080}")
    private String baseUrl;
    @Value("${app.account.mail-from:no-reply@booknet.local}")
    private String from;

    public void send(String email, String token, AccountTokenPurpose purpose) { deliver(email,token,purpose,null); }
    @Override public void sendInvitation(String email,String token,String personalMessage){deliver(email,token,AccountTokenPurpose.INVITE_ACCOUNT,personalMessage);}
    private void deliver(String email,String token,AccountTokenPurpose purpose,String personalMessage) {
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
        if(purpose==AccountTokenPurpose.INVITE_ACCOUNT){
            message.setSubject("BookNet - Activate your account");
            message.setText("You have been invited to BookNet. Set your password and activate your account using this link (valid for 24 hours, one use only):\n"+url
                +(personalMessage==null || personalMessage.isBlank() ? "" : "\n\nMessage from your library:\n"+personalMessage)
                +"\nIf you did not expect this invitation, contact your library.");
        }
        mailSender.sendAction(message, "ACCOUNT SECURITY", verification ? "Verify email address"
                : purpose == AccountTokenPurpose.INVITE_ACCOUNT ? "Activate account" : "Set new password", url);
    }
}
