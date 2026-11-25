package grp3.java.booknet.repository.book_service;

import grp3.java.booknet.entity.book_service.LoanItem;
import grp3.java.booknet.enums.book_service.LoanItemStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface LoanItemRepository extends JpaRepository<LoanItem, Long> {

    @org.springframework.data.jpa.repository.Query("""
        select i.loan.member.id as memberId, count(i) as onLoan,
               sum(case when i.dueDate < :today then 1L else 0L end) as overdue
        from LoanItem i where i.loan.member.id in :memberIds
          and i.status in :statuses and i.returnedAt is null
        group by i.loan.member.id
        """)
    List<grp3.java.booknet.dto.member.MemberLoanTotals> summarizeMembers(
            Collection<Long> memberIds, Collection<LoanItemStatus> statuses, LocalDate today);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"loan", "copy", "copy.book", "copy.book.authors"})
    List<LoanItem> findByLoan_Member_IdAndStatusInAndReturnedAtIsNull(Long memberId, Collection<LoanItemStatus> statuses);

    long countByStatus(LoanItemStatus status);

    long countByStatusIn(Collection<LoanItemStatus> statuses);

    long countByStatusInAndDueDateBefore(Collection<LoanItemStatus> statuses, LocalDate date);

    Page<LoanItem> findByStatus(LoanItemStatus status, Pageable pageable);

    Page<LoanItem> findByStatusIn(Collection<LoanItemStatus> statuses, Pageable pageable);

    List<LoanItem> findByLoan_Member_IdAndStatusIn(Long memberId, Collection<LoanItemStatus> statuses);

    boolean existsByLoan_Member_IdAndStatusInAndDueDateBefore(Long memberId, Collection<LoanItemStatus> statuses, LocalDate date);

    Optional<LoanItem> findByCopy_BarcodeIgnoreCaseAndStatusIn(String barcode, Collection<LoanItemStatus> statuses);

    Optional<LoanItem> findByCopy_BarcodeAndStatusIn(String barcode, Collection<LoanItemStatus> statuses);

    List<LoanItem> findByLoan_Id(Long loanId);
}
