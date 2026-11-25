package grp3.java.booknet.dto.admin;

/**
 * Dữ liệu cho Librarian Dashboard (UC39 — /staff/dashboard).
 */
public record LibrarianDashboardSummary(

        // ── Tổng quan thư viện ────────────────────────────────────────────────
        long publishedTitles,
        long totalCopies,
        long borrowedCopies,
        long availableCopies,
        long overdueLoans,
        long pendingReservations,

        // ── Pending tasks (circulation desk) ─────────────────────────────────
        /** Phiếu mượn đang chờ giao sách (PENDING_HANDOVER / READY). */
        long awaitingHandover,
        /** Yêu cầu trả sách đang chờ xử lý (RETURN_PENDING). */
        long pendingReturns,
        /** Phiếu mượn quá hạn chưa xử lý. */
        long overdueNotProcessed,
        /** Phí phạt đã chốt chưa thu. */
        long unpaidFines

) {
    public int availablePercent() {
        long total = borrowedCopies + availableCopies;
        return total == 0 ? 0 : (int) Math.round(availableCopies * 100.0 / total);
    }
}
