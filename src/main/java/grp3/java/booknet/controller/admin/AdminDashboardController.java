package grp3.java.booknet.controller.admin;

import grp3.java.booknet.security.principal.CustomOAuth2User;
import grp3.java.booknet.security.dto.CustomUserDetails;
import grp3.java.booknet.service.admin.AdminDashboardService;
import grp3.java.booknet.service.admin.StaffDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminDashboardController {

    private static final DateTimeFormatter SNAPSHOT_FORMAT =
            DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm", Locale.ENGLISH);

    private final AdminDashboardService dashboardService;
    private final StaffDashboardService staffDashboardService;

    @GetMapping("/dashboard")
    public String dashboard(Authentication authentication, Model model) {
        model.addAttribute("dashboard", dashboardService.getSummary());
        model.addAttribute("adminName", displayName(authentication));
        model.addAttribute("activePage", "dashboard");
        model.addAttribute("snapshotAt", LocalDateTime.now().format(SNAPSHOT_FORMAT));

        // ── UC39: Recent admin activity (Người 7) ───────────────────────────
        model.addAttribute("recentActivity", staffDashboardService.getRecentAdminActivity());
        // ── UC39: Phân bố tài khoản theo role ──────────────────────────────
        model.addAttribute("roleDistribution", staffDashboardService.getRoleDistribution());
        // ── UC39: Fine reconciliation summary ──────────────────────────────
        model.addAttribute("fineReport", staffDashboardService.getReport(
                java.time.LocalDate.now().minusDays(30),
                java.time.LocalDate.now()
        ));

        return "admin/dashboard";
    }

    private static String displayName(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        if (principal instanceof CustomUserDetails localUser) {
            return localUser.getFullName();
        }
        if (principal instanceof CustomOAuth2User googleUser) {
            return googleUser.getFullName();
        }
        return authentication.getName();
    }
}
