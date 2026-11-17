package grp3.java.booknet.repository.book_service;

import grp3.java.booknet.entity.book_service.BorrowRequest;
import grp3.java.booknet.enums.book_service.BorrowRequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface BorrowRequestRepository extends JpaRepository<BorrowRequest, Long> {

    long countByStatus(BorrowRequestStatus status);

    Page<BorrowRequest> findByStatus(BorrowRequestStatus status, Pageable pageable);

    @Query("SELECT count(r) FROM BorrowRequest r WHERE r.status = :status")
    long countApprovedAwaitingHandover(@Param("status") BorrowRequestStatus status);

    @Query("SELECT r FROM BorrowRequest r WHERE r.status = :status ORDER BY r.approvedAt DESC")
    Page<BorrowRequest> findApprovedAwaitingHandover(@Param("status") BorrowRequestStatus status, Pageable pageable);

    List<BorrowRequest> findByMember_IdAndStatusIn(Long memberId, Collection<BorrowRequestStatus> statuses);
}
