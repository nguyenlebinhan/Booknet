package grp3.java.booknet.repository.tracking;

import grp3.java.booknet.entity.tracking.PageVisitLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PageVisitLogRepository extends JpaRepository<PageVisitLog, Long> {

    long countByVisitedAtAfter(LocalDateTime since);

    @Query("SELECT p.pageName, p.pagePath, COUNT(p) AS cnt FROM PageVisitLog p " +
           "GROUP BY p.pageName, p.pagePath ORDER BY cnt DESC")
    List<Object[]> topVisitedPages(Pageable pageable);

    @Query("SELECT p FROM PageVisitLog p WHERE " +
           "(:keyword IS NULL OR LOWER(p.userEmail) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "  OR LOWER(p.pageName) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "  OR LOWER(p.pagePath) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "  OR LOWER(p.location) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "AND (:role IS NULL OR p.userRole = :role) " +
           "AND (:from IS NULL OR p.visitedAt >= :from) " +
           "AND (:to IS NULL OR p.visitedAt <= :to) " +
           "ORDER BY p.visitedAt DESC")
    Page<PageVisitLog> search(
            @Param("keyword") String keyword,
            @Param("role") String role,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            Pageable pageable);

    @Query("SELECT p.location, COUNT(p) AS cnt FROM PageVisitLog p " +
           "WHERE p.location IS NOT NULL GROUP BY p.location ORDER BY cnt DESC")
    List<Object[]> groupByLocation(Pageable pageable);
}
