package grp3.java.booknet.service.notification;

import grp3.java.booknet.dto.notification.EmailComposerView;
import grp3.java.booknet.dto.notification.EmailSendForm;
import grp3.java.booknet.entity.notification.EmailLog;
import grp3.java.booknet.entity.notification.EmailTemplate;
import grp3.java.booknet.enums.notification.EmailTemplateType;

import java.util.List;

public interface EmailNotificationService {

    record SendBatchResult(
            int sentCount,
            int failedCount,
            int skippedDuplicateCount,
            List<String> errors
    ) {}

    void ensureDefaultTemplates();

    EmailComposerView getComposerView(EmailSendForm form, boolean searchOnly);

    EmailTemplate getTemplate(EmailTemplateType type);

    void saveTemplate(EmailTemplateType type, String subjectTemplate, String bodyTemplate);

    EmailTemplate resetTemplate(EmailTemplateType type);

    SendBatchResult sendEmails(EmailSendForm form, String staffEmail);

    EmailLog retryFailedLog(Long logId, String staffEmail);
}
