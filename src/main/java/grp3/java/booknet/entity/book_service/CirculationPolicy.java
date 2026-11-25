package grp3.java.booknet.entity.book_service;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Chính sách lưu thông thư viện — Singleton (chỉ lưu 1 row, id = 1).
 * Admin quản lý qua UC44 (Configure Circulation Policies).
 */
@Entity
@Table(name = "circulation_policies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CirculationPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ── Quy định mượn ────────────────────────────────────────────────────────

    /** Số bản sao tối đa mỗi thành viên được mượn cùng lúc. Default: 5. */
    @Builder.Default
    @Column(name = "max_loans_per_member", nullable = false)
    private int maxLoansPerMember = 5;

    /** Số ngày mượn tiêu chuẩn. Default: 14. */
    @Builder.Default
    @Column(name = "loan_period_days", nullable = false)
    private int loanPeriodDays = 14;

    /** Số ngày gia hạn mỗi lần. Default: 7. */
    @Builder.Default
    @Column(name = "renew_period_days", nullable = false)
    private int renewPeriodDays = 7;

    // ── Phí phạt ─────────────────────────────────────────────────────────────

    /** Đơn giá phạt quá hạn (VND/ngày/bản sao). Default: 5000. */
    @Builder.Default
    @Column(name = "daily_fine_rate", nullable = false, precision = 10, scale = 2)
    private BigDecimal dailyFineRate = new BigDecimal("5000.00");

    // ── Đặt trước ────────────────────────────────────────────────────────────

    /** Số ngày giữ chỗ sau khi sách READY. Default: 2. */
    @Builder.Default
    @Column(name = "reservation_hold_days", nullable = false)
    private int reservationHoldDays = 2;

    // ── Audit ────────────────────────────────────────────────────────────────

    @Column(name = "updated_by", length = 150)
    private String updatedBy; // email của Admin cập nhật

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
