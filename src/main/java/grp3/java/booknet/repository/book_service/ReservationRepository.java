package grp3.java.booknet.repository.book_service;

import grp3.java.booknet.entity.book_service.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation,Long> {
}
