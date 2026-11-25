package grp3.java.booknet.service.contact;

import grp3.java.booknet.dto.contact.ContactForm;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import grp3.java.booknet.service.mail.BooknetMailService;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@RequiredArgsConstructor
public class ContactMailService {
    private final BooknetMailService mailSender;

    @Value("${app.account.mail-from:no-reply@booknet.local}")
    private String from;
    @Value("${app.contact.mail-to:}")
    private String to;

    public void send(@Valid ContactForm form) {
        if (to.isBlank()) {
            throw new MailSendException("Contact recipient is not configured.");
        }
        String topic = form.subject().isBlank() ? "Other" : form.subject();
        String phone = form.phone().isBlank() ? "Not provided" : form.phone();
        String text = "Name: " + form.fullName() + "\nEmail: " + form.email()
                + "\nPhone: " + phone + "\nTopic: " + topic + "\n\n" + form.message();
        var mail = new SimpleMailMessage();
        mail.setFrom(from);
        mail.setTo(to);
        mail.setReplyTo(form.email());
        mail.setSubject("BookNet - Contact: " + topic);
        mail.setText(text);
        mailSender.sendContact(mail, form);
    }
}
