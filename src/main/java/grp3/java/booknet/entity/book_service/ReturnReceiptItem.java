package grp3.java.booknet.entity.book_service;

import grp3.java.booknet.entity.BaseEntity;
import grp3.java.booknet.entity.book.BookCopy;
import grp3.java.booknet.enums.book_service.ReturnCondition;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@Entity
@Table(name = "return_receipt_items")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @SuperBuilder
@ToString(onlyExplicitlyIncluded = true)
public class ReturnReceiptItem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @ToString.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "receipt_id", nullable = false)
    private ReturnReceipt receipt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_item_id")
    private LoanItem loanItem;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "copy_id", nullable = false)
    private BookCopy copy;

    @Enumerated(EnumType.STRING)
    @Column(name = "item_condition", length = 20)
    private ReturnCondition itemCondition;

    @Enumerated(EnumType.STRING)
    @Column(name = "book_condition", length = 20)
    private ReturnCondition condition;

    @Column(name = "overdue_days")
    @Builder.Default
    private int overdueDays = 0;

    @Column(name = "condition_notes", length = 500)
    private String conditionNotes;

    @Column(name = "note", length = 500)
    private String note;

    @Column(name = "fine_amount", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal fineAmount = BigDecimal.ZERO;
}
