package grp3.java.booknet.repository.book_service;

import grp3.java.booknet.entity.book_service.Loan;
import grp3.java.booknet.enums.book_service.LoanStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface LoanRepository extends JpaRepository<Loan, Long> {

    long countByMember_IdAndStatusIn(Long memberId, Collection<LoanStatus> statuses);

    List<Loan> findByMember_IdAndStatus(Long memberId, LoanStatus status);
}

