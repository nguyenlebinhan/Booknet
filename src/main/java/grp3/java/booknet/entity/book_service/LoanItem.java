package grp3.java.booknet.entity.book_service;

import grp3.java.booknet.entity.BaseEntity;
import grp3.java.booknet.entity.book.BookCopy;
import grp3.java.booknet.enums.book_service.LoanItemStatus;
import grp3.java.booknet.enums.book_service.ReturnCondition;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "loan_items", indexes = {
        @Index(name = "idx_loan_items_status", columnList = "status"),
        @Index(name = "idx_loan_items_copy", columnList = "copy_id")
})
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @SuperBuilder
@ToString(onlyExplicitlyIncluded = true)
public class LoanItem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @ToString.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "loan_id", nullable = false)
    private Loan loan;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "copy_id", nullable = false)
    private BookCopy copy;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "returned_at")
    private LocalDateTime returnedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "return_condition", length = 20)
    private ReturnCondition returnCondition;

    @Column(name = "damage_notes", length = 500)
    private String damageNotes;

    @Column(name = "fine_amount", precision = 12, scale = 2)
    private BigDecimal fineAmount;

    @Column(name = "renew_count", nullable = false)
    @Builder.Default
    private int renewCount = 0;

    @Column(name = "last_renewed_at")
    private LocalDateTime lastRenewedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private LoanItemStatus status = LoanItemStatus.BORROWING;
}
