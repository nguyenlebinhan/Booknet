package grp3.java.booknet.controller.admin;

import grp3.java.booknet.entity.tracking.PageVisitLog;
import grp3.java.booknet.entity.tracking.UserLoginLog;
import grp3.java.booknet.security.dto.CustomUserDetails;
import grp3.java.booknet.security.principal.CustomOAuth2User;
import grp3.java.booknet.service.tracking.TrackingService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller for Admin Tracking Dashboard (Week 5 Step 4).
 * Shows login history and page navigation logs for ADMIN role.
 * Route: /admin/tracking
 */
@Controller
@RequestMapping("/admin/tracking")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminTrackingController {

    private final TrackingService trackingService;

    private static final int PAGE_SIZE = 15;

    @GetMapping
    public String trackingDashboard(
            @RequestParam(value = "tab", defaultValue = "logins") String tab,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "role", required = false) String role,
            @RequestParam(value = "from", required = false)
                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(value = "to", required = false)
                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(value = "loginPage", defaultValue = "0") int loginPageNum,
            @RequestParam(value = "visitPage", defaultValue = "0") int visitPageNum,
            Authentication authentication,
            Model model
    ) {
        String activeTab = "visits".equals(tab) ? "visits" : "logins";

        // Convert date filters to LocalDateTime range
        LocalDateTime fromDt = fromDate != null ? fromDate.atStartOfDay() : null;
        LocalDateTime toDt   = toDate   != null ? toDate.atTime(23, 59, 59) : null;

        // KPI stats
        model.addAttribute("totalLogins",      trackingService.totalLogins());
        model.addAttribute("loginsToday",       trackingService.loginsToday());
        model.addAttribute("uniqueActiveUsers", trackingService.uniqueActiveUsers());
        model.addAttribute("totalPageVisits",   trackingService.totalPageVisits());

        // Top visited pages: row[0]=pageName, row[1]=pagePath, row[2]=count
        List<Object[]> topPages = trackingService.topVisitedPages(6);
        model.addAttribute("topPages", topPages);

        long maxPageCount = topPages.isEmpty() ? 1L :
                ((Number) topPages.get(0)[2]).longValue();
        model.addAttribute("maxPageCount", Math.max(maxPageCount, 1L));

        // Location distributions: row[0]=location, row[1]=count
        List<Object[]> loginLocations = trackingService.loginLocationDistribution(6);
        List<Object[]> visitLocations = trackingService.visitLocationDistribution(6);
        model.addAttribute("loginLocations", loginLocations);
        model.addAttribute("visitLocations", visitLocations);

        long maxLoginLoc = loginLocations.isEmpty() ? 1L :
                ((Number) loginLocations.get(0)[1]).longValue();
        long maxVisitLoc = visitLocations.isEmpty() ? 1L :
                ((Number) visitLocations.get(0)[1]).longValue();
        model.addAttribute("maxLoginLoc", Math.max(maxLoginLoc, 1L));
        model.addAttribute("maxVisitLoc", Math.max(maxVisitLoc, 1L));

        // Paginated login history + concise device/browser labels
        Page<UserLoginLog> loginLogs = trackingService.searchLogins(
                keyword, role, fromDt, toDt,
                PageRequest.of(Math.max(0, loginPageNum), PAGE_SIZE));
        Map<Long, String> deviceLabels = new HashMap<>();
        for (UserLoginLog entry : loginLogs.getContent()) {
            if (entry.getId() != null) {
                deviceLabels.put(entry.getId(), formatDevice(entry.getUserAgent()));
            }
        }
        model.addAttribute("loginLogs", loginLogs);
        model.addAttribute("deviceLabels", deviceLabels);

        // Paginated page visit logs
        Page<PageVisitLog> visitLogs = trackingService.searchVisits(
                keyword, role, fromDt, toDt,
                PageRequest.of(Math.max(0, visitPageNum), PAGE_SIZE));
        model.addAttribute("visitLogs", visitLogs);

        // Filters for form state
        model.addAttribute("activeTab",  activeTab);
        model.addAttribute("keyword",    keyword);
        model.addAttribute("filterRole", role);
        model.addAttribute("fromDate",   fromDate);
        model.addAttribute("toDate",     toDate);

        // Layout
        model.addAttribute("activePage", "tracking");
        model.addAttribute("adminName",  displayName(authentication));

        return "admin/tracking/index";
    }

    private static String displayName(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        if (principal instanceof CustomUserDetails u) return u.getFullName();
        if (principal instanceof CustomOAuth2User u)  return u.getFullName();
        return authentication.getName();
    }

    /**
     * Parses a raw User-Agent string into a concise "Browser · OS" label for table display.
     */
    public static String formatDevice(String userAgent) {
        if (userAgent == null || userAgent.isBlank() || "Unknown".equalsIgnoreCase(userAgent.trim())) {
            return "Unknown";
        }
        String ua = userAgent.trim();
        String browser = detectBrowser(ua);
        String os = detectOs(ua);

        if (browser != null && os != null) {
            return browser + " \u00b7 " + os;
        }
        if (browser != null) {
            return browser;
        }
        if (os != null) {
            return os;
        }
        return ua.length() > 30 ? ua.substring(0, 27) + "..." : ua;
    }

    private static String detectBrowser(String ua) {
        if (ua.contains("Edg/") || ua.contains("EdgA/") || ua.contains("EdgiOS/") || ua.contains("Edge/")) {
            return "Edge";
        }
        if (ua.contains("OPR/") || ua.contains("Opera")) {
            return "Opera";
        }
        if (ua.contains("Firefox/") || ua.contains("FxiOS/")) {
            return "Firefox";
        }
        if (ua.contains("Chrome/") || ua.contains("CriOS/")) {
            return "Chrome";
        }
        if (ua.contains("Safari/")) {
            return "Safari";
        }
        if (ua.contains("MSIE") || ua.contains("Trident/")) {
            return "Internet Explorer";
        }
        if (ua.startsWith("curl/")) {
            return "cURL";
        }
        if (ua.contains("PostmanRuntime")) {
            return "Postman";
        }
        return null;
    }

    private static String detectOs(String ua) {
        if (ua.contains("Windows")) {
            return "Windows";
        }
        if (ua.contains("Android")) {
            return "Android";
        }
        if (ua.contains("iPhone") || ua.contains("iPad") || ua.contains("iPod")) {
            return "iOS";
        }
        if (ua.contains("Mac OS X") || ua.contains("Macintosh")) {
            return "macOS";
        }
        if (ua.contains("Linux") || ua.contains("X11")) {
            return "Linux";
        }
        return null;
    }
}
