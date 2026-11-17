package grp3.java.booknet.repository.book_service;

import grp3.java.booknet.entity.book_service.ReturnReceiptItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReturnReceiptItemRepository extends JpaRepository<ReturnReceiptItem, Long> {

    List<ReturnReceiptItem> findByReceipt_Id(Long receiptId);
}
