package grp3.java.booknet.service.mail;

import grp3.java.booknet.dto.contact.ContactForm;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriUtils;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

/** Shared presentation and UTF-8 multipart delivery for all outgoing library emails. */
@Service
@RequiredArgsConstructor
public class BooknetMailService {
    private final JavaMailSender sender;
    private final TemplateEngine templates;

    public void send(SimpleMailMessage mail, String category) {
        deliver(mail, category, null, null, null);
    }

    public void sendAction(SimpleMailMessage mail, String category, String label, String url) {
        deliver(mail, category, label, url, null);
    }

    public void sendContact(SimpleMailMessage mail, ContactForm contact) {
        String replyUrl = "mailto:" + UriUtils.encode(contact.email(), StandardCharsets.UTF_8).replace("%40", "@");
        deliver(mail, "LIBRARY CONTACT", "Reply to sender", replyUrl, contact);
    }

    private void deliver(SimpleMailMessage mail, String category, String actionLabel, String actionUrl,
                         ContactForm contact) {
        Context context = new Context(Locale.ENGLISH);
        context.setVariable("category", category);
        context.setVariable("title", contact == null ? mail.getSubject() : "A new message for the library");
        context.setVariable("body", contact == null ? mail.getText() : contact.message());
        context.setVariable("contact", contact);
        context.setVariable("topic", contact == null ? null : contact.subject().isBlank() ? "Other" : contact.subject());
        context.setVariable("phone", contact == null ? null : contact.phone().isBlank() ? "Not provided" : contact.phone());
        context.setVariable("actionLabel", actionLabel);
        context.setVariable("actionUrl", actionUrl);
        String html = templates.process("mail/message", context);
        try {
            var message = sender.createMimeMessage();
            var helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
            helper.setFrom(mail.getFrom());
            helper.setTo(mail.getTo());
            if (mail.getReplyTo() != null) helper.setReplyTo(mail.getReplyTo());
            helper.setSubject(mail.getSubject());
            helper.setText(mail.getText(), html);
            sender.send(message);
        } catch (MessagingException ex) {
            throw new MailSendException("Unable to prepare Booknet email.", ex);
        }
    }
}
