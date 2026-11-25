package grp3.java.booknet.entity.tracking;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Records each successful login event for admin tracking (Step 4, Week 5).
 * Table: user_login_logs
 */
@Entity
@Table(name = "user_login_logs", indexes = {
        @Index(name = "idx_login_at", columnList = "login_at"),
        @Index(name = "idx_login_email", columnList = "user_email")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserLoginLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_email", nullable = false, length = 150)
    private String userEmail;

    @Column(name = "user_name", length = 150)
    private String userName;

    @Column(name = "user_role", length = 30)
    private String userRole;

    @Column(name = "login_at", nullable = false)
    private LocalDateTime loginAt;

    @Column(name = "ip_address", length = 60)
    private String ipAddress;

    @Column(name = "location", length = 150)
    private String location;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    /** "PASSWORD" or "GOOGLE_OAUTH2" */
    @Column(name = "login_method", length = 30)
    private String loginMethod;

    @Column(name = "session_id", length = 100)
    private String sessionId;

    @PrePersist
    protected void prePersist() {
        if (this.loginAt == null) {
            this.loginAt = LocalDateTime.now();
        }
    }
}
