package grp3.java.booknet.repository.book_service;

import grp3.java.booknet.entity.book_service.Loan;
import grp3.java.booknet.enums.book_service.LoanStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.time.LocalDate;

@Repository
public interface LoanRepository extends JpaRepository<Loan, Long> {

    long countByDueDateBeforeAndStatusIn(LocalDate dueDate, Collection<LoanStatus> statuses);

    long countByMember_IdAndStatusIn(Long memberId, Collection<LoanStatus> statuses);

    List<Loan> findByMember_IdAndStatus(Long memberId, LoanStatus status);

    List<Loan> findByMember_IdAndStatusInOrderByBorrowedAtDesc(Long memberId, Collection<LoanStatus> statuses);

    List<Loan> findByMember_IdAndStatusOrderByReturnedAtDesc(Long memberId, LoanStatus status);

    List<Loan> findByMember_IdOrderByBorrowedAtDesc(Long memberId);

    // ── Người 7: Dashboard / Reports ─────────────────────────────────────────

    /** 10 giao dịch mượn/trả gần nhất (cho Recent activity trên Librarian dashboard). */
    List<Loan> findTop10ByOrderByUpdatedAtDesc();

    /** Đếm theo status (count bản sao đang mượn, đang quá hạn, ...). */
    long countByStatus(LoanStatus status);

    /** Đếm loans được tạo trong khoảng thời gian (cho reports). */
    long countByBorrowedAtBetween(java.time.LocalDateTime from, java.time.LocalDateTime to);

    /** Đếm loans được trả trong khoảng thời gian. */
    long countByReturnedAtBetween(java.time.LocalDateTime from, java.time.LocalDateTime to);

    /** Danh sách loans được trả trong khoảng thời gian (dùng để tính tỷ lệ đúng hạn & phân bố quá hạn). */
    List<Loan> findByReturnedAtBetween(java.time.LocalDateTime from, java.time.LocalDateTime to);

    /** Top sách được mượn nhiều nhất. */
    @org.springframework.data.jpa.repository.Query("""
        SELECT l.copy.book.title, COUNT(l) as cnt
        FROM Loan l
        WHERE l.borrowedAt BETWEEN :from AND :to
        GROUP BY l.copy.book.id, l.copy.book.title
        ORDER BY cnt DESC
        """)
    List<Object[]> findTopBorrowedTitles(
            @org.springframework.data.repository.query.Param("from") java.time.LocalDateTime from,
            @org.springframework.data.repository.query.Param("to") java.time.LocalDateTime to,
            org.springframework.data.domain.Pageable pageable);
}
