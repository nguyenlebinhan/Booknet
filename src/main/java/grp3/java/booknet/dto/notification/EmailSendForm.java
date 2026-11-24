package grp3.java.booknet.dto.notification;

import grp3.java.booknet.enums.notification.EmailTemplateType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailSendForm {

    @NotNull(message = "Please choose a notification template.")
    @Builder.Default
    private EmailTemplateType templateType = EmailTemplateType.OVERDUE;

    @NotBlank
    @Builder.Default
    private String recipientMode = "SPECIFIC";

    private Long selectedMemberId;

    private Long selectedReferenceId;

    private String searchKeyword;

    @NotBlank(message = "Email subject is required.")
    @Size(max = 250, message = "Email subject cannot exceed 250 characters.")
    private String subjectTemplate;

    @NotBlank(message = "Email message body is required.")
    @Size(max = 10000, message = "Email body cannot exceed 10000 characters.")
    private String bodyTemplate;

    private String idempotencyToken;

    @Builder.Default
    private boolean reviewStep = false;
}
