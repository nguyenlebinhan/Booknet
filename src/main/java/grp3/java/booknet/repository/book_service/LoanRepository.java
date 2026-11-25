package grp3.java.booknet.repository.book_service;

import grp3.java.booknet.entity.book_service.Loan;
import grp3.java.booknet.enums.book_service.LoanStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.time.LocalDate;

@Repository
public interface LoanRepository extends JpaRepository<Loan, Long> {

    @org.springframework.data.jpa.repository.Query("""
        select l.member.id as memberId, count(l) as onLoan,
               sum(case when l.dueDate < :today then 1L else 0L end) as overdue
        from Loan l where l.member.id in :memberIds and l.status in :statuses
          and l.returnedAt is null and not exists (select i.id from LoanItem i where i.loan = l)
        group by l.member.id
        """)
    List<grp3.java.booknet.dto.member.MemberLoanTotals> summarizeLegacyMembers(
            Collection<Long> memberIds, Collection<LoanStatus> statuses, LocalDate today);

    @EntityGraph(attributePaths = {"copy", "copy.book", "copy.book.authors"})
    @org.springframework.data.jpa.repository.Query("""
        select l from Loan l where l.member.id = :memberId and l.status in :statuses
          and l.returnedAt is null and not exists (select i.id from LoanItem i where i.loan = l)
        """)
    List<Loan> findLegacyActiveByMember(Long memberId, Collection<LoanStatus> statuses);

    long countByDueDateBeforeAndStatusIn(LocalDate dueDate, Collection<LoanStatus> statuses);

    long countByMember_IdAndStatusIn(Long memberId, Collection<LoanStatus> statuses);

    List<Loan> findByMember_IdAndStatus(Long memberId, LoanStatus status);

    List<Loan> findByMember_IdAndStatusInOrderByBorrowedAtDesc(Long memberId, Collection<LoanStatus> statuses);

    List<Loan> findByMember_IdAndStatusOrderByReturnedAtDesc(Long memberId, LoanStatus status);

    List<Loan> findByMember_IdOrderByBorrowedAtDesc(Long memberId);

    // ── Người 7: Dashboard / Reports ─────────────────────────────────────────

    /** 10 giao dich muon/tra gan nhat (cho Recent activity tren Librarian dashboard). */
    List<Loan> findTop10ByOrderByUpdatedAtDesc();

    /** Dem theo status (count ban sao dang muon, dang qua han, ...). */
    long countByStatus(LoanStatus status);

    /** Dem loans duoc tao trong khoang thoi gian (cho reports). */
    long countByBorrowedAtBetween(java.time.LocalDateTime from, java.time.LocalDateTime to);

    /** Dem loans duoc tra trong khoang thoi gian. */
    long countByReturnedAtBetween(java.time.LocalDateTime from, java.time.LocalDateTime to);

    /** Danh sach loans duoc tra trong khoang thoi gian. */
    List<Loan> findByReturnedAtBetween(java.time.LocalDateTime from, java.time.LocalDateTime to);

    /** Top sach duoc muon nhieu nhat. */
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

    // ── origin/main: Circulation features ────────────────────────────────────

    boolean existsByMember_IdAndCopy_Book_IdAndStatusIn(Long memberId, Long bookId, Collection<LoanStatus> statuses);

    List<Loan> findByCopy_Book_IdAndStatusInOrderByDueDateAsc(Long bookId, Collection<LoanStatus> statuses);

    @EntityGraph(attributePaths = {"member", "member.memberCard", "copy", "copy.book", "copy.book.authors"})
    List<Loan> findByStatusInAndDueDateBetweenOrderByDueDateAsc(Collection<LoanStatus> statuses, LocalDate start, LocalDate end);

    @EntityGraph(attributePaths = {"member", "member.memberCard", "copy", "copy.book", "copy.book.authors"})
    List<Loan> findByStatusInAndDueDateBeforeOrderByDueDateAsc(Collection<LoanStatus> statuses, LocalDate cutoff);

    @EntityGraph(attributePaths = {"member", "member.memberCard", "copy", "copy.book", "copy.book.authors"})
    List<Loan> findByStatusInOrderByDueDateAsc(Collection<LoanStatus> statuses);
}
