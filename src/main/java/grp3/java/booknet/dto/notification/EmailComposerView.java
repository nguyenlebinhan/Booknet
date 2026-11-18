package grp3.java.booknet.dto.notification;

import grp3.java.booknet.enums.notification.EmailLogStatus;
import grp3.java.booknet.enums.notification.EmailTemplateType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record EmailComposerView(
        List<EmailTemplateDto> templates,
        EmailTemplateType activeTemplateType,
        String recipientMode,
        List<RecipientOptionDto> memberChoices,
        List<RecipientOptionDto> eligibleGroupRecipients,
        RecipientOptionDto selectedRecipient,
        EmailPreviewResult preview,
        List<EmailLogRowDto> recentLogs,
        String referenceDateLabel
) {
    public record EmailTemplateDto(
            EmailTemplateType type,
            String name,
            String description,
            String subjectTemplate,
            String bodyTemplate
    ) {}

    public record RecipientOptionDto(
            Long userId,
            String fullName,
            String initials,
            String email,
            String memberCode,
            boolean emailOptedIn,
            Long referenceId,
            String referenceType,
            String referenceCode,
            String bookTitle,
            String authors,
            String coverUrl,
            String dueDateFormatted,
            String daysOverdueText,
            String pickupUntilFormatted,
            String statusBadge,
            String statusTone,
            String contextNote,
            Map<String, String> variableValues
    ) {}

    public record EmailPreviewResult(
            boolean valid,
            List<String> unknownVariables,
            List<String> validationErrors,
            int validRecipientCount,
            int skippedDuplicateCount,
            int missingEmailCount,
            int optedOutCount,
            String fromAddress,
            String sampleRecipientEmail,
            String sampleRecipientName,
            String renderedSubject,
            String renderedBody,
            List<RecipientOptionDto> resolvedRecipients
    ) {}

    public record EmailLogRowDto(
            Long id,
            EmailTemplateType templateType,
            String templateName,
            String recipientName,
            String recipientEmail,
            String subject,
            String body,
            EmailLogStatus status,
            String errorMessage,
            int retryCount,
            LocalDateTime timestamp,
            String sentByName
    ) {}
}
