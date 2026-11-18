package grp3.java.booknet.entity.user;

import grp3.java.booknet.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

/** Card state is independent from account access and borrowing/payment state. */
@Entity
@Table(name = "member_cards")
@Getter @Setter @NoArgsConstructor
public class MemberCard extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @OneToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;
    @Column(name = "card_number", nullable = false, unique = true, length = 24)
    private String cardNumber;
    private LocalDate issuedOn;
    private LocalDate expiresOn;
    private Long activatedBy;

    public String status(LocalDate today) {
        if (issuedOn == null) return "Pending";
        return expiresOn != null && expiresOn.isBefore(today) ? "Expired" : "Active";
    }
}
