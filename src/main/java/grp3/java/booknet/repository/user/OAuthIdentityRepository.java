package grp3.java.booknet.repository.user;

import grp3.java.booknet.entity.user.OAuthIdentity;
import grp3.java.booknet.enums.user.AuthProvider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OAuthIdentityRepository extends JpaRepository<OAuthIdentity, Long> {
    Optional<OAuthIdentity> findByProviderAndProviderUserId(AuthProvider provider, String providerUserId);

    Optional<OAuthIdentity> findByUser_IdAndProvider(Long userId, AuthProvider provider);

    boolean existsByUser_IdAndProvider(Long userId, AuthProvider provider);
}
