package grp3.java.booknet.entity.notification;

import grp3.java.booknet.entity.BaseEntity;
import grp3.java.booknet.entity.user.User;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "notification_preferences")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class NotificationPreference extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "website_enabled", nullable = false)
    @Builder.Default
    private boolean websiteEnabled = true;

    @Column(name = "email_enabled", nullable = false)
    @Builder.Default
    private boolean emailEnabled = true;

    @Column(name = "events_newsletter_enabled", nullable = false)
    @Builder.Default
    private boolean eventsNewsletterEnabled = false;
}
