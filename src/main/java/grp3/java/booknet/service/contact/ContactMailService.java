package grp3.java.booknet.service.contact;

import grp3.java.booknet.dto.contact.ContactForm;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@RequiredArgsConstructor
public class ContactMailService {
    private final JavaMailSender mailSender;

    @Value("${app.account.mail-from:no-reply@booknet.local}")
    private String from;
    @Value("${app.contact.mail-to:}")
    private String to;

    public void send(@Valid ContactForm form) {
        if (to.isBlank()) {
            throw new MailSendException("Contact recipient is not configured.");
        }
        SimpleMailMessage mail = new SimpleMailMessage();
        mail.setFrom(from);
        mail.setTo(to);
        mail.setReplyTo(form.email());
        mail.setSubject("BookNet - Contact: " + (form.subject().isBlank() ? "Other" : form.subject()));
        mail.setText("Name: " + form.fullName() + "\nEmail: " + form.email()
                + "\nTopic: " + (form.subject().isBlank() ? "Other" : form.subject())
                + "\n\n" + form.message());
        mailSender.send(mail);
    }
}
