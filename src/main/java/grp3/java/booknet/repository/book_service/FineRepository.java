package grp3.java.booknet.repository.book_service;

import grp3.java.booknet.entity.book_service.Fine;
import grp3.java.booknet.enums.book_service.FineStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface FineRepository extends JpaRepository<Fine, Long> {

    // ── Giữ nguyên — đang được BorrowServiceImpl sử dụng ───────────────────
    boolean existsByMember_IdAndStatus(Long memberId, FineStatus status);
    List<Fine> findByMember_IdAndStatus(Long memberId, FineStatus status);

    // ── Tìm theo Loan ────────────────────────────────────────────────────────
    List<Fine> findByLoan_Id(Long loanId);

    // ── Tìm theo Member ──────────────────────────────────────────────────────
    List<Fine> findByMember_Id(Long memberId);
    List<Fine> findByMember_IdOrderByCreatedAtDesc(Long memberId);

    // ── Mã phiếu ────────────────────────────────────────────────────────────
    Optional<Fine> findByFineCode(String fineCode);

    // ── Search & Filter cho Librarian / Admin (UC29) ──────────────────────
    @Query("""
        SELECT f FROM Fine f
        WHERE (:status IS NULL OR f.status = :status)
          AND (:keyword IS NULL OR :keyword = ''
               OR LOWER(f.member.fullName) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(f.member.email) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(f.member.memberCode) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(f.fineCode) LIKE LOWER(CONCAT('%', :keyword, '%')))
        ORDER BY f.createdAt DESC
        """)
    List<Fine> searchFines(@Param("status") FineStatus status, @Param("keyword") String keyword);

    // ── Aggregate cho reports ────────────────────────────────────────────────
    @Query("SELECT COALESCE(SUM(f.amount), 0) FROM Fine f WHERE f.status = :status")
    BigDecimal sumAmountByStatus(@Param("status") FineStatus status);

    @Query("SELECT COALESCE(SUM(f.amount), 0) FROM Fine f WHERE f.member.id = :memberId AND f.status = :status")
    BigDecimal sumAmountByMemberAndStatus(@Param("memberId") Long memberId, @Param("status") FineStatus status);

    @Query("SELECT COUNT(f) FROM Fine f WHERE f.status = :status")
    long countByStatus(@Param("status") FineStatus status);

    // ── Dùng cho dashboard — đếm số fine chốt chưa thu trong khoảng thời gian ─
    @Query("SELECT COUNT(f) FROM Fine f WHERE f.status = :status AND f.finalizedAt >= :from AND f.finalizedAt < :to")
    long countByStatusAndFinalizedAtBetween(
            @Param("status") FineStatus status,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);
}
