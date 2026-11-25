package grp3.java.booknet.repository;

import grp3.java.booknet.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    /** 10 bản ghi mới nhất cho hộp "Recent administrative activity" trên dashboard. */
    List<AuditLog> findTop10ByOrderByOccurredAtDesc();

    /** Phân trang toàn bộ log cho trang /admin/audit-logs. */
    Page<AuditLog> findAllByOrderByOccurredAtDesc(Pageable pageable);

    /** Tìm kiếm theo keyword (email, action, targetType, description) và action. */
    @Query("""
        SELECT a FROM AuditLog a WHERE
           (:keyword IS NULL OR :keyword = ''
            OR LOWER(a.actorEmail) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(a.action) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(a.targetType) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(a.description) LIKE LOWER(CONCAT('%', :keyword, '%')))
           AND (:action IS NULL OR :action = '' OR a.action = :action)
        ORDER BY a.occurredAt DESC
        """)
    Page<AuditLog> searchWithKeyword(
            @Param("keyword") String keyword,
            @Param("action") String action,
            Pageable pageable);

    /** Lọc theo actor + khoảng thời gian (cho bộ lọc trang audit-log). */
    @Query("SELECT a FROM AuditLog a WHERE " +
           "(:actorEmail IS NULL OR a.actorEmail = :actorEmail) AND " +
           "(:action IS NULL OR a.action = :action) AND " +
           "(:from IS NULL OR a.occurredAt >= :from) AND " +
           "(:to IS NULL OR a.occurredAt < :to) " +
           "ORDER BY a.occurredAt DESC")
    Page<AuditLog> search(
            @Param("actorEmail") String actorEmail,
            @Param("action") String action,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            Pageable pageable);
}
