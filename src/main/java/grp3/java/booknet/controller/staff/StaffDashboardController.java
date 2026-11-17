package grp3.java.booknet.controller.staff;

import grp3.java.booknet.security.principal.CustomOAuth2User;
import grp3.java.booknet.security.dto.CustomUserDetails;
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

/**
 * Dashboard cho LIBRARIAN (UC39) — /staff/dashboard.
 * ADMIN cũng có thể xem (hasAnyRole).
 */
@Controller
@RequestMapping("/staff")
@PreAuthorize("hasAnyRole('LIBRARIAN', 'ADMIN')")
@RequiredArgsConstructor
public class StaffDashboardController {

    private static final DateTimeFormatter SNAPSHOT_FORMAT =
            DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm", Locale.ENGLISH);

    private final StaffDashboardService staffDashboardService;

    @GetMapping("/dashboard")
    public String librarianDashboard(Authentication authentication, Model model) {
        model.addAttribute("summary", staffDashboardService.getLibrarianSummary());
        model.addAttribute("recentActivity", staffDashboardService.getRecentLoanActivity());
        model.addAttribute("adminName", displayName(authentication));
        model.addAttribute("activePage", "dashboard");
        model.addAttribute("snapshotAt", LocalDateTime.now().format(SNAPSHOT_FORMAT));
        return "staff/dashboard";
    }

    private static String displayName(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        if (principal instanceof CustomUserDetails u) return u.getFullName();
        if (principal instanceof CustomOAuth2User u) return u.getFullName();
        return authentication.getName();
    }
}
