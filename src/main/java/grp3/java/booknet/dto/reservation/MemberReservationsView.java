package grp3.java.booknet.dto.reservation;

import grp3.java.booknet.enums.book_service.ReservationStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record MemberReservationsView(
        List<ReservationCardDto> activeReservations,
        List<ReservationCardDto> completedReservations,
        List<ReservationCardDto> canceledReservations,
        ReadyPickupBanner readyBanner,
        int activeCount,
        int completedCount,
        int canceledCount,
        int maxActiveReservations,
        ReservationCardDto cancelCandidate
) {
    public record ReadyPickupBanner(
            String bookTitle,
            LocalDateTime pickupUntil,
            String memberCode
    ) {}

    public record ReservationCardDto(
            Long reservationId,
            String reservationCode,
            Long bookId,
            String bookCode,
            String bookTitle,
            String authors,
            String category,
            String coverUrl,
            ReservationStatus status,
            LocalDateTime reservedAt,
            LocalDateTime readyAt,
            LocalDateTime pickupUntil,
            String heldCopyBarcode,
            String pickupLocation,
            int queuePosition,
            int totalInQueue,
            LocalDate estimatedAvailabilityDate,
            LocalDateTime closedAt,
            String notes,
            boolean canCancel
    ) {
        public String paddedQueuePosition() {
            return String.format("%02d", Math.max(queuePosition, 1));
        }

        public String paddedTotalInQueue() {
            return String.format("%02d", Math.max(totalInQueue, 1));
        }
    }
}
