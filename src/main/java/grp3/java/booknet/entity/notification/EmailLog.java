package grp3.java.booknet.entity.notification;

import grp3.java.booknet.entity.BaseEntity;
import grp3.java.booknet.enums.notification.EmailLogStatus;
import grp3.java.booknet.enums.notification.EmailTemplateType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Entity
@Table(name = "email_logs", indexes = {
        @Index(name = "idx_email_log_created", columnList = "created_at"),
        @Index(name = "idx_email_log_idempotency", columnList = "idempotency_key")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class EmailLog extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "template_type", nullable = false, length = 30)
    private EmailTemplateType templateType;

    @Column(name = "template_name", nullable = false, length = 100)
    private String templateName;

    @Column(name = "recipient_user_id")
    private Long recipientUserId;

    @Column(name = "recipient_name", nullable = false, length = 120)
    private String recipientName;

    @Column(name = "recipient_email", nullable = false, length = 160)
    private String recipientEmail;

    @Column(name = "sent_by_user_id")
    private Long sentByUserId;

    @Column(name = "sent_by_name", length = 120)
    private String sentByName;

    @Column(nullable = false, length = 250)
    private String subject;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String body;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EmailLogStatus status;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    @Column(name = "reference_type", length = 30)
    private String referenceType;

    @Column(name = "reference_id")
    private Long referenceId;

    @Column(name = "idempotency_key", length = 120)
    private String idempotencyKey;

    @Column(name = "retry_count", nullable = false)
    @Builder.Default
    private int retryCount = 0;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "last_attempt_at", nullable = false)
    private LocalDateTime lastAttemptAt;
}
