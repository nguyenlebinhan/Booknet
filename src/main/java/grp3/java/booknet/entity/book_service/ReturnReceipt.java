package grp3.java.booknet.entity.book_service;

import grp3.java.booknet.entity.BaseEntity;
import grp3.java.booknet.entity.user.User;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "return_receipts", indexes = {
        @Index(name = "idx_receipt_code", columnList = "receipt_code", unique = true),
        @Index(name = "idx_receipt_member", columnList = "member_id")
})
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @SuperBuilder
@ToString(onlyExplicitlyIncluded = true)
public class ReturnReceipt extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @ToString.Include
    private Long id;

    @Column(name = "receipt_code", length = 50, unique = true, nullable = false)
    private String receiptCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_id")
    private Loan loan;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private User member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "received_by")
    private User receivedBy;

    @Column(name = "return_date")
    private LocalDateTime returnDate;

    @Column(name = "returned_at")
    private LocalDateTime returnedAt;

    @Column(name = "total_fines", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal totalFines = BigDecimal.ZERO;

    @Column(name = "total_fine_amount", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal totalFineAmount = BigDecimal.ZERO;

    @Column(name = "notes", length = 500)
    private String notes;

    @Column(name = "note", length = 500)
    private String note;

    @OneToMany(mappedBy = "receipt", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ReturnReceiptItem> items = new ArrayList<>();

    public void addItem(ReturnReceiptItem item) {
        items.add(item);
        item.setReceipt(this);
    }
}
