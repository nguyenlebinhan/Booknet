package grp3.java.booknet.entity.tracking;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Records each page navigation event for admin tracking (Step 4, Week 5).
 * Table: page_visit_logs
 */
@Entity
@Table(name = "page_visit_logs", indexes = {
        @Index(name = "idx_visit_at", columnList = "visited_at"),
        @Index(name = "idx_visit_email", columnList = "user_email"),
        @Index(name = "idx_visit_path", columnList = "page_path")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PageVisitLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Email of logged-in user, or "Anonymous (Guest)" if unauthenticated. */
    @Column(name = "user_email", nullable = false, length = 150)
    private String userEmail;

    /** GUEST, MEMBER, LIBRARIAN, ADMIN */
    @Column(name = "user_role", length = 30)
    private String userRole;

    /** URL path, e.g. /catalog, /member/fines */
    @Column(name = "page_path", nullable = false, length = 500)
    private String pagePath;

    /** Friendly page name, e.g. "Book Catalog", "Overdue Fines" */
    @Column(name = "page_name", length = 150)
    private String pageName;

    @Column(name = "http_method", length = 10)
    private String httpMethod;

    @Column(name = "ip_address", length = 60)
    private String ipAddress;

    @Column(name = "location", length = 150)
    private String location;

    @Column(name = "visited_at", nullable = false)
    private LocalDateTime visitedAt;

    @Column(name = "session_id", length = 100)
    private String sessionId;

    @PrePersist
    protected void prePersist() {
        if (this.visitedAt == null) {
            this.visitedAt = LocalDateTime.now();
        }
    }
}
