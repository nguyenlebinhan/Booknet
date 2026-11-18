package grp3.java.booknet.repository.user;

import grp3.java.booknet.entity.user.AccountAccessAudit;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountAccessAuditRepository extends JpaRepository<AccountAccessAudit, Long> { }
