package grp3.java.booknet;

import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.List;

import static org.mockito.Mockito.*;

/** Inspect the actual multipart email while keeping existing token/body assertions readable. */
final class TestMail {
    static void prepare(JavaMailSender sender) {
        when(sender.createMimeMessage()).thenAnswer(invocation -> new MimeMessage((Session) null));
    }

    static List<SimpleMailMessage> messages(JavaMailSender sender) {
        return mockingDetails(sender).getInvocations().stream()
                .filter(i -> i.getMethod().getName().equals("send") && i.getArgument(0) instanceof MimeMessage)
                .map(i -> plain(i.getArgument(0))).toList();
    }

    static SimpleMailMessage plain(MimeMessage mime) {
        try {
            mime.saveChanges();
            var message = new SimpleMailMessage();
            message.setTo(java.util.Arrays.stream(mime.getAllRecipients()).map(Object::toString).toArray(String[]::new));
            message.setFrom(mime.getFrom()[0].toString());
            message.setSubject(mime.getSubject());
            message.setText(content(mime, "text/plain"));
            return message;
        } catch (Exception ex) {
            throw new AssertionError("Could not read multipart email", ex);
        }
    }

    static String content(Part part, String type) throws Exception {
        if (part.isMimeType(type)) return (String) part.getContent();
        if (part.getContent() instanceof Multipart multipart) {
            for (int i = 0; i < multipart.getCount(); i++) {
                String value = content(multipart.getBodyPart(i), type);
                if (!value.isEmpty()) return value;
            }
        }
        return "";
    }
}
