package grp3.java.booknet.controller.staff;

import grp3.java.booknet.security.principal.CustomOAuth2User;
import grp3.java.booknet.security.dto.CustomUserDetails;
import grp3.java.booknet.service.admin.StaffDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;

/**
 * Báo cáo lưu thông (UC32) — /staff/reports.
 * LIBRARIAN & ADMIN.
 */
@Controller
@RequestMapping("/staff/reports")
@PreAuthorize("hasAnyRole('LIBRARIAN', 'ADMIN')")
@RequiredArgsConstructor
public class StaffReportsController {

    private final StaffDashboardService staffDashboardService;

    @GetMapping
    public String reports(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            Authentication authentication,
            Model model
    ) {
        // Default: 30 ngày gần nhất
        if (to == null) to = LocalDate.now();
        if (from == null) from = to.minusDays(29);

        model.addAttribute("report", staffDashboardService.getReport(from, to));
        model.addAttribute("fromDate", from);
        model.addAttribute("toDate", to);
        model.addAttribute("adminName", displayName(authentication));
        model.addAttribute("activePage", "reports");
        return "staff/reports";
    }

    private static String displayName(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        if (principal instanceof CustomUserDetails u) return u.getFullName();
        if (principal instanceof CustomOAuth2User u) return u.getFullName();
        return authentication.getName();
    }
}
