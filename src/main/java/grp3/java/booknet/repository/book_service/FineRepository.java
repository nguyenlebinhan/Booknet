package grp3.java.booknet.repository.book_service;

import grp3.java.booknet.entity.book_service.Fine;
import grp3.java.booknet.enums.book_service.FineStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FineRepository extends JpaRepository<Fine, Long> {

    boolean existsByMember_IdAndStatus(Long memberId, FineStatus status);

    List<Fine> findByMember_IdAndStatus(Long memberId, FineStatus status);
}

