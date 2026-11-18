package grp3.java.booknet.repository.notification;

import grp3.java.booknet.entity.notification.EmailLog;
import grp3.java.booknet.enums.notification.EmailLogStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmailLogRepository extends JpaRepository<EmailLog, Long> {

    List<EmailLog> findTop50ByOrderByCreatedAtDescIdDesc();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from EmailLog e where e.id = :id")
    Optional<EmailLog> findLockedById(@Param("id") Long id);

    boolean existsByIdempotencyKeyAndStatus(String idempotencyKey, EmailLogStatus status);
}
