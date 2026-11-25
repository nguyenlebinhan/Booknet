package grp3.java.booknet.service.admin;

import grp3.java.booknet.dto.admin.RecentActivityItem;
import grp3.java.booknet.dto.admin.ReportSummary;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface StaffDashboardService {

    /** 10 giao dịch gần nhất trên Librarian dashboard. */
    List<RecentActivityItem> getRecentLoanActivity();

    /** 10 audit log gần nhất cho Admin dashboard (UC39). */
    List<RecentActivityItem> getRecentAdminActivity();

    /**
     * Báo cáo thống kê theo khoảng thời gian (UC32).
     *
     * @param from ngày bắt đầu (bao gồm)
     * @param to   ngày kết thúc (bao gồm)
     */
    ReportSummary getReport(LocalDate from, LocalDate to);

    /** Phân bố số tài khoản theo role {roleLabel: count}. */
    Map<String, Long> getRoleDistribution();
}
