package grp3.java.booknet;

import grp3.java.booknet.enums.user.AccountTokenPurpose;
import grp3.java.booknet.service.account.AccountMailService;
import grp3.java.booknet.service.mail.BooknetMailService;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
class BooknetMailRenderingTests {
    @Autowired AccountMailService accounts;
    @Autowired BooknetMailService mail;
    @MockitoBean JavaMailSender sender;

    @BeforeEach void setup() {
        reset(sender);
        TestMail.prepare(sender);
    }

    @ParameterizedTest
    @EnumSource(AccountTokenPurpose.class)
    void accountEmailsKeepLinksAndExpiryInsideSharedDesign(AccountTokenPurpose purpose) throws Exception {
        if (purpose == AccountTokenPurpose.INVITE_ACCOUNT) {
            accounts.sendInvitation("reader@example.com", "preview-token", "Welcome <reader>\nPlease join us.");
        } else {
            accounts.send("reader@example.com", "preview-token", purpose);
        }
        MimeMessage mime = delivered();
        String html = TestMail.content(mime, "text/html");
        String text = TestMail.content(mime, "text/plain");
        assertThat(html).contains("booknet", "#173f35", "#f8f6f0", "ACCOUNT SECURITY", "preview-token")
                .doesNotContain(" th:", "Contact details", "Reply-To:");
        boolean verification = purpose == AccountTokenPurpose.VERIFY_EMAIL;
        String endpoint = verification ? "/verify-email?token=" : "/reset-password?token=";
        assertThat(html).contains(endpoint + "preview-token");
        assertThat(text).contains(endpoint + "preview-token",
                purpose == AccountTokenPurpose.RESET_PASSWORD ? "15 minutes" : "24 hours");
        if (purpose == AccountTokenPurpose.INVITE_ACCOUNT) {
            assertThat(html).contains("Welcome &lt;reader&gt;").doesNotContain("Welcome <reader>");
        }
        save(purpose.name().toLowerCase() + ".html", html);
    }

    @Test void notificationsUseSharedDesignWithoutInventingAnActionOrChangingBody() throws Exception {
        var message = new SimpleMailMessage();
        message.setFrom("library@booknet.test");
        message.setTo("reader@example.com");
        message.setSubject("Overdue books");
        String body = "Hello Nguyễn Minh Anh,\n\nYour book <Book & title> is overdue.\nPlease return it to the library.";
        message.setText(body);
        mail.send(message, "LIBRARY NOTIFICATION");
        MimeMessage mime = delivered();
        String html = TestMail.content(mime, "text/html");
        assertThat(TestMail.content(mime, "text/plain")).isEqualTo(body);
        assertThat(html).contains("LIBRARY NOTIFICATION", "#173f35", "Nguyễn Minh Anh", "&lt;Book &amp; title&gt;",
                "white-space:pre-wrap").doesNotContain("Contact details", "Reply to sender", "<Book & title>");
        save("notification.html", html);
    }

    private MimeMessage delivered() throws Exception {
        var captor = org.mockito.ArgumentCaptor.forClass(MimeMessage.class);
        verify(sender).send(captor.capture());
        var mime = captor.getValue();
        mime.saveChanges();
        assertThat(mime.getAllRecipients()[0].toString()).isEqualTo("reader@example.com");
        return mime;
    }

    private void save(String filename, String html) throws Exception {
        Path directory = Path.of("target/mail-review");
        Files.createDirectories(directory);
        Files.writeString(directory.resolve(filename), html);
    }
}
