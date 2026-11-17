package grp3.java.booknet.service.admin;

import grp3.java.booknet.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

public interface AuditLogService {

    /** Ghi một bản ghi audit. actorRole có thể null nếu không cần. */
    AuditLog log(String actorEmail, String actorRole,
                 String action,
                 String targetType, Long targetId,
                 String description);

    /** Lấy 10 bản ghi mới nhất cho dashboard. */
    List<AuditLog> getRecent10();

    /** Toàn bộ log phân trang (cho /admin/audit-logs). */
    Page<AuditLog> findAll(Pageable pageable);

    /** Tìm kiếm theo keyword và action cho trang /admin/audit-logs. */
    Page<AuditLog> searchWithKeyword(String keyword, String action, Pageable pageable);

    /** Tìm kiếm có lọc. */
    Page<AuditLog> search(String actorEmail, String action,
                          LocalDateTime from, LocalDateTime to,
                          Pageable pageable);
}
