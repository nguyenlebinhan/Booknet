package grp3.java.booknet;

import grp3.java.booknet.dto.ContactForm;
import grp3.java.booknet.service.ContactMailService;
import org.junit.jupiter.api.Test;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class ContactMailServiceTests {
    @Test
    void missingRecipientFailsBeforeSubmittingAnyEmail() {
        JavaMailSender sender = mock(JavaMailSender.class);
        ContactMailService service = new ContactMailService(sender);
        ReflectionTestUtils.setField(service, "to", "");
        assertThatThrownBy(() -> service.send(new ContactForm("Reader", "reader@example.com", "Other", "Help")))
                .isInstanceOf(MailSendException.class).hasMessage("Contact recipient is not configured.");
        verifyNoInteractions(sender);
    }
}
