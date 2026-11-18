package grp3.java.booknet.entity.user;

import grp3.java.booknet.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

/** Scalar IDs intentionally retain the historical actor/target after account changes. */
@Entity @Table(name = "account_access_audits")
@Getter @Setter @NoArgsConstructor
public class AccountAccessAudit extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false) private Long actorId;
    @Column(nullable = false) private Long targetId;
    @Column(nullable = false, length = 30) private String action;
    @Column(nullable = false, length = 100) private String previousValue;
    @Column(nullable = false, length = 100) private String newValue;
    @Column(length=300) private String reason;
}
