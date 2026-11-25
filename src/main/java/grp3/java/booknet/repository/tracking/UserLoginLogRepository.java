package grp3.java.booknet.repository.tracking;

import grp3.java.booknet.entity.tracking.UserLoginLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface UserLoginLogRepository extends JpaRepository<UserLoginLog, Long> {

    long countByLoginAtAfter(LocalDateTime since);

    @Query("SELECT COUNT(DISTINCT l.userEmail) FROM UserLoginLog l WHERE l.loginAt >= :since")
    long countDistinctEmailSince(@Param("since") LocalDateTime since);

    @Query("SELECT l FROM UserLoginLog l WHERE " +
           "(:keyword IS NULL OR LOWER(l.userEmail) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "  OR LOWER(l.userName) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "  OR LOWER(l.location) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "AND (:role IS NULL OR l.userRole = :role) " +
           "AND (:from IS NULL OR l.loginAt >= :from) " +
           "AND (:to IS NULL OR l.loginAt <= :to) " +
           "ORDER BY l.loginAt DESC")
    Page<UserLoginLog> search(
            @Param("keyword") String keyword,
            @Param("role") String role,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            Pageable pageable);

    @Query("SELECT l.location, COUNT(l) AS cnt FROM UserLoginLog l " +
           "WHERE l.location IS NOT NULL GROUP BY l.location ORDER BY cnt DESC")
    List<Object[]> groupByLocation(Pageable pageable);
}
