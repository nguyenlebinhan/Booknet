package grp3.java.booknet.repository.book_service;

import grp3.java.booknet.entity.book_service.BorrowRequestItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BorrowRequestItemRepository extends JpaRepository<BorrowRequestItem, Long> {

    List<BorrowRequestItem> findByRequest_Id(Long requestId);
}
