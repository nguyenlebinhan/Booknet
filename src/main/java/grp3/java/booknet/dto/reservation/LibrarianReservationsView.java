package grp3.java.booknet.dto.reservation;

import grp3.java.booknet.enums.book_service.ReservationStatus;

import java.time.LocalDateTime;
import java.util.List;

public record LibrarianReservationsView(
        long pendingCount,
        long readyCount,
        long overdueHoldCount,
        long closedCount,
        String currentTab,
        String keyword,
        List<StaffReservationRow> rows,
        List<BookQueueGroup> bookQueues
) {
    public record StaffReservationRow(
            Long reservationId,
            String reservationCode,
            ReservationStatus status,
            Long bookId,
            String bookCode,
            String bookTitle,
            long availableCopiesCount,
            Long memberId,
            String memberName,
            String memberEmail,
            String memberCode,
            LocalDateTime reservedAt,
            LocalDateTime readyAt,
            LocalDateTime pickupUntil,
            boolean pickupExpired,
            String assignedCopyBarcode,
            String assignedCopyShelf,
            int queuePosition,
            int totalInQueue,
            String notes,
            LocalDateTime closedAt
    ) {}

    public record BookQueueGroup(
            Long bookId,
            String bookCode,
            String bookTitle,
            long availableCopiesCount,
            long totalCopiesCount,
            long pendingCount,
            long readyCount,
            List<StaffReservationRow> queueEntries
    ) {}
}
