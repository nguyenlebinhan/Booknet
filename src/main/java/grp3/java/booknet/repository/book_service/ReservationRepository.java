package grp3.java.booknet.repository.book_service;

import grp3.java.booknet.entity.book_service.Reservation;
import grp3.java.booknet.enums.book_service.ReservationStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    long countByStatus(ReservationStatus status);

    long countByBook_IdAndStatus(Long bookId, ReservationStatus status);

    long countByMember_IdAndStatusIn(Long memberId, Collection<ReservationStatus> statuses);

    List<Reservation> findByMember_IdAndStatusIn(Long memberId, Collection<ReservationStatus> statuses);

    @EntityGraph(attributePaths = {"book", "book.authors", "book.categories", "assignedCopy", "member"})
    List<Reservation> findByMember_IdOrderByReservedAtDescIdDesc(Long memberId);

    boolean existsByMember_IdAndBook_IdAndStatusIn(Long memberId, Long bookId, Collection<ReservationStatus> statuses);

    boolean existsByBook_IdAndStatusInAndMember_IdNot(Long bookId, Collection<ReservationStatus> statuses, Long memberId);

    boolean existsByBook_IdAndStatusIn(Long bookId, Collection<ReservationStatus> statuses);

    @EntityGraph(attributePaths = {"book", "member", "assignedCopy"})
    List<Reservation> findByBook_IdAndStatusOrderByReservedAtAscIdAsc(Long bookId, ReservationStatus status);

    @EntityGraph(attributePaths = {"book", "member", "assignedCopy"})
    Optional<Reservation> findFirstByBook_IdAndStatusOrderByReservedAtAscIdAsc(Long bookId, ReservationStatus status);

    @EntityGraph(attributePaths = {"book", "member", "assignedCopy"})
    List<Reservation> findByStatusAndPickupUntilBefore(ReservationStatus status, LocalDateTime cutoff);

    @EntityGraph(attributePaths = {"book", "member", "assignedCopy"})
    List<Reservation> findByStatusOrderByReadyAtAscIdAsc(ReservationStatus status);

    @EntityGraph(attributePaths = {"book", "member", "assignedCopy"})
    List<Reservation> findAllByOrderByReservedAtDescIdDesc();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Reservation r where r.id = :id")
    Optional<Reservation> findLockedById(@Param("id") Long id);

    @Query("""
            select count(r) from Reservation r
            where r.book.id = :bookId
              and r.status = grp3.java.booknet.enums.book_service.ReservationStatus.PENDING
              and (r.reservedAt < :reservedAt or (r.reservedAt = :reservedAt and r.id < :reservationId))
            """)
    long countAheadInQueue(@Param("bookId") Long bookId,
                           @Param("reservedAt") LocalDateTime reservedAt,
                           @Param("reservationId") Long reservationId);
}
