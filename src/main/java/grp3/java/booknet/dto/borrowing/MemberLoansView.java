package grp3.java.booknet.dto.borrowing;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record MemberLoansView(
        String memberName,
        String memberCode,
        LocalDate updatedDate,
        int activeLoansCount,
        int maxAllowedLoans,
        int dueSoonCount,
        int overdueCount,
        int booksReadThisYear,
        String topAlertMessage,
        List<LoanItem> activeLoans,
        List<ReservationItem> pendingReservations,
        List<HistoryItem> historyLoans,
        RenewSuggestion renewSuggestion,
        List<FavoriteItem> favoriteBooks
) {
    public record FavoriteItem(
            Long bookId,
            String bookTitle,
            String authors,
            String coverUrl,
            String genre,
            boolean onShelf,
            long availableCopies,
            long totalCopies,
            LocalDateTime savedAt
    ) {}
    public record LoanItem(
            Long loanId,
            String loanCode,
            Long bookId,
            String bookTitle,
            String authors,
            String coverUrl,
            String barcode,
            LocalDate loanDate,
            LocalDate dueDate,
            String statusText,
            String dueBadgeText,
            String dueBadgeType, // "overdue", "due-soon", "normal"
            boolean canReturn,
            boolean isReturnPending,
            LocalDateTime returnRequestedAt,
            boolean canRenew,
            boolean isRenewPending,
            String renewalStatusHint
    ) {}

    /**
     * Loan request đã submit (Reservation.PENDING hoặc READY) —
     * đang chờ Librarian tiếp nhận / chuẩn bị sách.
     */
    public record ReservationItem(
            Long reservationId,
            String reservationCode,
            Long bookId,
            String bookTitle,
            String authors,
            String coverUrl,
            LocalDateTime requestedAt,
            ReservationStatus status
    ) {
        public enum ReservationStatus { PENDING, READY }
    }

    public record HistoryItem(
            Long loanId,
            String loanCode,
            String receiptCode,
            String bookTitle,
            String barcode,
            LocalDate borrowedDate,
            LocalDate dueDate,
            LocalDateTime returnRequestedAt,
            LocalDateTime returnedAt,
            String receivedByLibrarian,
            String statusBadge
    ) {}

    public record RenewSuggestion(
            Long loanId,
            String bookTitle,
            LocalDate estimatedNewDueDate,
            int renewalsUsed,
            int maxRenewals
    ) {}
}
