package grp3.java.booknet.repository.book_service;

import grp3.java.booknet.entity.book_service.ReturnReceipt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReturnReceiptRepository extends JpaRepository<ReturnReceipt, Long> {

    Optional<ReturnReceipt> findByReceiptCode(String receiptCode);
}
