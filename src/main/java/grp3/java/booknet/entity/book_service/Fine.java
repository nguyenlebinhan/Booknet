package grp3.java.booknet.entity.book_service;

import grp3.java.booknet.entity.BaseEntity;
import grp3.java.booknet.entity.user.User;
import grp3.java.booknet.enums.book_service.FineReason;
import grp3.java.booknet.enums.book_service.FineStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "fines")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @SuperBuilder
@ToString(onlyExplicitlyIncluded = true)
public class Fine extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @ToString.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "loan_id", nullable = false)
    private Loan loan;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private User member;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FineReason reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private FineStatus status = FineStatus.UNPAID;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    // ── Mã phiếu phạt (hiển thị / tra cứu) ─────────────────────────────────
    @Column(name = "fine_code", unique = true, length = 30)
    private String fineCode; // e.g. "FN-2026-0001"

    // ── Dữ liệu tính phí ────────────────────────────────────────────────────
    @Column(name = "overdue_days")
    private Integer overdueDays;

    /** Đơn giá/ngày lưu tại thời điểm chốt (snapshot — tránh thay đổi policy làm sai lịch sử). */
    @Column(name = "daily_rate", precision = 10, scale = 2)
    private BigDecimal dailyRate;

    // ── Chốt phí (Librarian nhận sách vật lý) ───────────────────────────────
    @Column(name = "finalized_at")
    private LocalDateTime finalizedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "finalized_by")
    private User finalizedBy;

    // ── Thanh toán ──────────────────────────────────────────────────────────
    /** "AT_DESK" hoặc "QR_CODE" */
    @Column(name = "payment_method", length = 20)
    private String paymentMethod;

    @Column(name = "transaction_ref", length = 100)
    private String transactionRef;

    @Column(name = "confirmed_by_staff_at")
    private LocalDateTime confirmedByStaffAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "confirmed_by_staff")
    private User confirmedByStaff;

    // ── Điều chỉnh / Waive (Admin) ───────────────────────────────────────────
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "adjusted_by")
    private User adjustedBy;

    @Column(name = "adjusted_at")
    private LocalDateTime adjustedAt;

    @Column(name = "adjustment_note", columnDefinition = "TEXT")
    private String adjustmentNote;
}
