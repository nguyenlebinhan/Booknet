package grp3.java.booknet.repository.user;

import grp3.java.booknet.entity.user.AccountToken;
import grp3.java.booknet.enums.user.AccountTokenPurpose;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;

public interface AccountTokenRepository extends JpaRepository<AccountToken, Long> {
    Optional<AccountToken> findByTokenHash(String tokenHash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from AccountToken t where t.tokenHash = :hash")
    Optional<AccountToken> findLockedByHash(@Param("hash") String hash);

    @Query("select t.user.id from AccountToken t where t.tokenHash = :hash")
    Optional<Long> findUserIdByHash(@Param("hash") String hash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<AccountToken> findByUser_IdAndPurposeAndUsedAtIsNull(Long userId, AccountTokenPurpose purpose);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<AccountToken> findFirstByUser_IdAndPurposeOrderByIssuedAtDesc(Long userId, AccountTokenPurpose purpose);
}
