package grp3.java.booknet.service.reservation;

import grp3.java.booknet.entity.book.BookCopy;
import grp3.java.booknet.entity.book_service.Reservation;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Shared boundary for reservation queue transitions triggered by circulation,
 * copy management, and scheduled expiration without coupling callers to ReservationService internals.
 */
public interface ReservationQueueService {

    boolean hasWaitingQueueForRenewal(Long bookId, Long borrowerMemberId);

    Optional<Reservation> assignCopyToNextPendingReservation(BookCopy copy, LocalDateTime now);

    int expireOverdueReservations();

    int expireOverdueReservations(LocalDateTime asOf);
}
