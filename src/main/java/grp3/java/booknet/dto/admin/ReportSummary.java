package grp3.java.booknet.dto.admin;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Dữ liệu tổng hợp cho trang Reports (UC32) — /staff/reports.
 */
public record ReportSummary(

        // ── Khoảng thời gian đang xem ─────────────────────────────────────────
        String fromDate,
        String toDate,

        // ── Thống kê lượt mượn/trả ────────────────────────────────────────────
        long totalLoaned,
        long totalReturned,
        long onTimeReturns,
        long lateReturns,

        // ── Phân bố sách quá hạn ──────────────────────────────────────────────
        /** 1–3 ngày */
        long overdueGroup1to3,
        /** 4–7 ngày */
        long overdueGroup4to7,
        /** trên 7 ngày */
        long overdueGroupOver7,

        // ── Top 5 đầu sách ────────────────────────────────────────────────────
        /** Danh sách [{title, count}] */
        List<TitleStat> top5Titles,

        // ── Tình trạng tiền phạt ──────────────────────────────────────────────
        BigDecimal finePaid,
        BigDecimal fineUnpaid,
        BigDecimal fineEstimated

) {
    public record TitleStat(String title, long count) {}

    /** % trả đúng hạn (0–100). */
    public int onTimePercent() {
        if (totalReturned == 0) return 100;
        return (int) Math.round(onTimeReturns * 100.0 / totalReturned);
    }

    public long totalOverdue() {
        return overdueGroup1to3 + overdueGroup4to7 + overdueGroupOver7;
    }
}
