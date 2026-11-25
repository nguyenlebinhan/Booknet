package grp3.java.booknet.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Ghi nhận mỗi hành động quan trọng trong hệ thống (UC43).
 * <p>
 * KHÔNG extends BaseEntity để tránh circular dependency với AuditingEntityListener
 * và để giữ occurredAt do chính entity tự set (không phụ thuộc Spring Data Auditing).
 */
@Entity
@Table(name = "audit_logs", indexes = {
        @Index(name = "idx_audit_occurred_at", columnList = "occurred_at DESC"),
        @Index(name = "idx_audit_actor", columnList = "actor_email"),
        @Index(name = "idx_audit_target", columnList = "target_type, target_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(onlyExplicitlyIncluded = true)
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @ToString.Include
    private Long id;

    // ── Người thực hiện ───────────────────────────────────────────────────────
    /** Email của user thực hiện (không dùng FK để tránh cascade issue khi xóa user). */
    @Column(name = "actor_email", nullable = false, length = 150)
    private String actorEmail;

    @Column(name = "actor_role", length = 30)
    private String actorRole; // e.g. "ADMIN", "LIBRARIAN"

    // ── Hành động ─────────────────────────────────────────────────────────────
    /**
     * Mã hành động ngắn gọn, uppercase.
     * Ví dụ: ROLE_CHANGED, ACCOUNT_SUSPENDED, FINE_WAIVED, FINE_CONFIRMED,
     *         BOOK_PUBLISHED, BOOK_UNPUBLISHED, RETURN_CONFIRMED, POLICY_UPDATED
     */
    @Column(name = "action", nullable = false, length = 60)
    private String action;

    // ── Đối tượng bị tác động ─────────────────────────────────────────────────
    /** Loại entity bị ảnh hưởng. Ví dụ: "User", "Fine", "Book", "Loan", "CirculationPolicy" */
    @Column(name = "target_type", length = 50)
    private String targetType;

    /** ID của entity bị ảnh hưởng (nullable khi action không gắn entity cụ thể). */
    @Column(name = "target_id")
    private Long targetId;

    /** Mô tả chi tiết — human-readable. */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    // ── Thời điểm ─────────────────────────────────────────────────────────────
    @Column(name = "occurred_at", nullable = false, updatable = false)
    private LocalDateTime occurredAt;

    @PrePersist
    protected void prePersist() {
        if (this.occurredAt == null) {
            this.occurredAt = LocalDateTime.now();
        }
    }
}
