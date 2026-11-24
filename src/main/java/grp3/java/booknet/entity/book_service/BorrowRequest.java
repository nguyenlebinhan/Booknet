package grp3.java.booknet.entity.book_service;

import grp3.java.booknet.entity.BaseEntity;
import grp3.java.booknet.entity.user.User;
import grp3.java.booknet.enums.book_service.BorrowRequestStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "borrow_requests", indexes = {
        @Index(name = "idx_br_member_status", columnList = "member_id, status"),
        @Index(name = "idx_br_request_code", columnList = "request_code", unique = true)
})
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @SuperBuilder
@ToString(onlyExplicitlyIncluded = true)
public class BorrowRequest extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @ToString.Include
    private Long id;

    @Column(name = "request_code", length = 50, unique = true)
    private String requestCode;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private User member;

    @Column(name = "request_date", updatable = false)
    private LocalDateTime requestDate;

    @PrePersist
    protected void onRequest() {
        if (this.requestDate == null) {
            this.requestDate = LocalDateTime.now();
        }
        if (this.requestCode == null) {
            this.requestCode = "BR-" + System.currentTimeMillis();
        }
    }

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private BorrowRequestStatus status = BorrowRequestStatus.PENDING;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private User approvedBy;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    @Column(name = "notes", length = 1000)
    private String notes;

    @OneToMany(mappedBy = "request", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<BorrowRequestItem> items = new ArrayList<>();

    public void addItem(BorrowRequestItem item) {
        items.add(item);
        item.setRequest(this);
    }
}
