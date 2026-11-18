package grp3.java.booknet.repository.notification;

import grp3.java.booknet.entity.notification.Notification;
import grp3.java.booknet.enums.notification.NotificationType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    @EntityGraph(attributePaths = {"loan", "loan.copy", "loan.copy.book", "reservation", "reservation.book"})
    List<Notification> findByUser_IdOrderByCreatedAtDescIdDesc(Long userId);

    @EntityGraph(attributePaths = {"loan", "loan.copy", "loan.copy.book", "reservation", "reservation.book"})
    List<Notification> findByUser_IdAndReadFalseOrderByCreatedAtDescIdDesc(Long userId);

    @EntityGraph(attributePaths = {"loan", "loan.copy", "loan.copy.book", "reservation", "reservation.book"})
    List<Notification> findByUser_IdAndTypeInOrderByCreatedAtDescIdDesc(Long userId, Collection<NotificationType> types);

    long countByUser_IdAndReadFalse(Long userId);

    Optional<Notification> findByIdAndUser_Id(Long id, Long userId);

    @Modifying
    @Query("update Notification n set n.read = true where n.user.id = :userId and n.read = false")
    int markAllAsReadByUserId(@Param("userId") Long userId);

    boolean existsByUser_IdAndReservation_IdAndType(Long userId, Long reservationId, NotificationType type);

    boolean existsByUser_IdAndLoan_IdAndType(Long userId, Long loanId, NotificationType type);

    boolean existsByUser_IdAndLoan_IdAndTypeAndCreatedAtAfter(Long userId, Long loanId, NotificationType type, LocalDateTime since);
}
