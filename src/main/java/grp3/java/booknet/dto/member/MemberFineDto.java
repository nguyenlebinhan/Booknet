package grp3.java.booknet.dto.member;

import grp3.java.booknet.enums.book_service.FineStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO đại diện cho một khoản phí phạt hiển thị cho Member (UC20).
 * Bao gồm cả các khoản ước tính (ESTIMATED) của sách đang mượn quá hạn.
 */
public record MemberFineDto(
        Long fineId,
        Long loanId,
        String fineCode,
        String bookTitle,
        String authorNames,
        String coverUrl,
        String copyBarcode,
        LocalDate borrowedDate,
        LocalDate dueDate,
        LocalDate returnedDate,
        long overdueDays,
        BigDecimal amount,
        FineStatus status
) {
    public boolean isEstimated() {
        return status == FineStatus.ESTIMATED;
    }

    public boolean isUnpaid() {
        return status == FineStatus.UNPAID;
    }

    public boolean isPaid() {
        return status == FineStatus.PAID;
    }

    public boolean isWaived() {
        return status == FineStatus.WAIVED;
    }

    public boolean canPay() {
        return isUnpaid();
    }

    public String statusLabel() {
        return switch (status) {
            case ESTIMATED -> "Estimated · Not finalized";
            case UNPAID    -> "Finalized · Unpaid";
            case PAID      -> "Paid";
            case WAIVED    -> "Waived";
        };
    }

    public String badgeClass() {
        return switch (status) {
            case ESTIMATED -> "badge-estimated";
            case UNPAID    -> "badge-unpaid";
            case PAID      -> "badge-paid";
            case WAIVED    -> "badge-waived";
        };
    }
}
