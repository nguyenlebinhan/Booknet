package grp3.java.booknet.repository.book_service;

import grp3.java.booknet.entity.book_service.Reservation;
import grp3.java.booknet.enums.book_service.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    long countByMember_IdAndStatusIn(Long memberId, Collection<ReservationStatus> statuses);

    List<Reservation> findByMember_IdAndStatusIn(Long memberId, Collection<ReservationStatus> statuses);

    boolean existsByMember_IdAndBook_IdAndStatusIn(Long memberId, Long bookId, Collection<ReservationStatus> statuses);
}

