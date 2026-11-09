package grp3.java.booknet.entity.user;

import grp3.java.booknet.enums.user.AccountTokenPurpose;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "account_tokens", indexes = @Index(name = "idx_account_token_user", columnList = "user_id,purpose"))
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class AccountToken {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AccountTokenPurpose purpose;

    @Column(nullable = false)
    private Instant issuedAt;
    @Column(nullable = false)
    private Instant expiresAt;
    private Instant usedAt;
}
