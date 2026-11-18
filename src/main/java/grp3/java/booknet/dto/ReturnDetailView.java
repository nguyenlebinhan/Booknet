package grp3.java.booknet.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * ViewModel cho trang "Returns & receipts" — trang 2 cột:
 *  - Trái : chi tiết phiếu mượn + form xác nhận (Librarian)
 *  - Phải : receipt sau khi Librarian đã xác nhận
 */
public record ReturnDetailView(

        /* ── Thông tin phiếu mượn ── */
        Long   loanId,
        String loanCode,
        String bookTitle,
        String authors,
        String coverUrl,
        String barcode,
        String copyCode,        // "KH003-04" kiểu barcode rút gọn

        LocalDateTime borrowedAt,
        LocalDate     dueDate,
        LocalDateTime returnRequestedAt,

        String loanStatusName,  // BORROWING / RETURN_PENDING / RETURNED / OVERDUE …
        String dueBadgeText,    // "2 days overdue" | "On time" | …
        String dueBadgeType,    // "overdue" | "normal" | "due-soon"

        /* ── Thông tin member ── */
        String memberName,
        String memberCode,

        /* ── Receipt (chỉ có khi status == RETURNED) ── */
        boolean isReturned,
        String  receiptCode,
        LocalDateTime returnedAt,
        String  librarianName,
        int     booksStillOnLoan,

        /* ── Existing condition note (nếu librarian đã ghi) ── */
        String conditionNote
) {}
