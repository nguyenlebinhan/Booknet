package grp3.java.booknet.entity.user;

import grp3.java.booknet.entity.BaseEntity;
import grp3.java.booknet.enums.user.AuthProvider;
import grp3.java.booknet.enums.user.UserStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@ToString(onlyExplicitlyIncluded = true)
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @ToString.Include
    private Long id;

    @Column(name = "full_name", nullable = false, length = 120)
    private String fullName;

    @ToString.Include
    @Column(nullable = false, unique = true, length = 120)
    private String email;

    @Column(length = 50)
    private String username;

    @Column(length = 100)
    private String password;

    @Column(nullable = false, length = 30)
    private String role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AuthProvider provider;

    @Column(nullable = false)
    private boolean enabled;

    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserStatus status;

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    @Column(name = "member_code", unique = true, length = 20)
    private String memberCode;

    @Column(length = 20)
    private String phone;

    private String address;

    @PrePersist
    private void initDefaults() {
        if (this.role == null) this.role = "ROLE_USER";
        if (this.provider == null) this.provider = AuthProvider.LOCAL;
        if (this.status == null) this.status = UserStatus.ACTIVE;
        this.enabled = true;
        this.emailVerified = true;
    }
}
