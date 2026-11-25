package grp3.java.booknet.config.tracking;

import grp3.java.booknet.entity.tracking.PageVisitLog;
import grp3.java.booknet.security.dto.CustomUserDetails;
import grp3.java.booknet.security.principal.CustomOAuth2User;
import grp3.java.booknet.service.tracking.IpLocationResolver;
import grp3.java.booknet.service.tracking.TrackingService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Intercepts HTTP GET requests to HTML pages and records them as PageVisitLog entries.
 * Skips static resources, OAuth2 handshake redirects, API endpoints, and non-HTML requests.
 * Always returns true to never block any request.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PageTrackingInterceptor implements HandlerInterceptor {

    private final TrackingService trackingService;
    private final IpLocationResolver ipLocationResolver;

    private static final String[] SKIP_PREFIXES = {
            "/css/", "/js/", "/images/", "/fonts/", "/webjars/",
            "/covers/", "/favicon.ico", "/error", "/api/", "/actuator/",
            "/oauth2/", "/login/oauth2/"
    };

    @Override
    public boolean preHandle(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull Object handler
    ) {
        try {
            if (!"GET".equalsIgnoreCase(request.getMethod())) {
                return true;
            }

            String path = request.getRequestURI();
            String contextPath = request.getContextPath();
            if (contextPath != null && !contextPath.isEmpty() && path.startsWith(contextPath)) {
                path = path.substring(contextPath.length());
            }
            if (path.isEmpty()) {
                path = "/";
            }

            for (String prefix : SKIP_PREFIXES) {
                if (path.startsWith(prefix) || path.equals(prefix.stripTrailing())) {
                    return true;
                }
            }

            // Skip AJAX / fetch API requests
            if ("XMLHttpRequest".equalsIgnoreCase(request.getHeader("X-Requested-With"))) {
                return true;
            }

            String acceptHeader = request.getHeader("Accept");
            if (acceptHeader != null && !acceptHeader.contains("text/html")
                    && !acceptHeader.contains("*/*")) {
                return true;
            }

            // Extract authenticated user email and role
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String userEmail = "Anonymous (Guest)";
            String userRole = "GUEST";

            if (auth != null
                    && auth.isAuthenticated()
                    && !(auth instanceof AnonymousAuthenticationToken)
                    && !"anonymousUser".equals(auth.getPrincipal())) {
                userEmail = resolveEmail(auth);
                userRole = auth.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .filter(a -> a != null && a.startsWith("ROLE_"))
                        .map(a -> a.substring("ROLE_".length()))
                        .findFirst()
                        .orElse("MEMBER");
            }

            String pageName = resolvePageName(path);
            String ipAddress = ipLocationResolver.extractIp(request);
            String location = ipLocationResolver.resolve(ipAddress);

            String sessionId = null;
            if (request.getSession(false) != null) {
                sessionId = request.getSession(false).getId();
            }

            PageVisitLog visitLog = PageVisitLog.builder()
                    .userEmail(truncate(userEmail, 150))
                    .userRole(truncate(userRole, 30))
                    .pagePath(truncate(path, 500))
                    .pageName(truncate(pageName, 150))
                    .httpMethod(truncate(request.getMethod(), 10))
                    .ipAddress(truncate(ipAddress, 60))
                    .location(truncate(location, 150))
                    .sessionId(truncate(sessionId, 100))
                    .build();

            trackingService.savePageVisit(visitLog);

        } catch (Exception ex) {
            log.debug("Page tracking failed for {}: {}", request.getRequestURI(), ex.getMessage());
        }

        return true;
    }

    private static String resolveEmail(Authentication auth) {
        Object principal = auth.getPrincipal();
        if (principal instanceof CustomUserDetails u && u.getEmail() != null) {
            return u.getEmail();
        }
        if (principal instanceof CustomOAuth2User u && u.getEmail() != null) {
            return u.getEmail();
        }
        return auth.getName() != null ? auth.getName() : "unknown";
    }

    private String resolvePageName(String path) {
        switch (path) {
            case "/":
            case "/home":
                return "Home Page";
            case "/catalog":
                return "Book Catalog";
            case "/login":
                return "Login Page";
            case "/register":
                return "Member Registration";
            case "/forgot-password":
                return "Forgot Password";
            case "/reset-password":
                return "Reset Password";
            case "/verify-email":
                return "Email Verification";
            case "/about":
                return "About";
            case "/contact":
                return "Contact";
            case "/assistant":
                return "AI Assistant";
            case "/member/my-books":
                return "My Borrowed Books";
            case "/member/fines":
                return "Overdue Fines";
            case "/member/reservations":
                return "My Reservations";
            case "/staff/dashboard":
                return "Librarian Dashboard";
            case "/staff/loans":
                return "Loans & Returns";
            case "/staff/members":
                return "Member Directory";
            case "/staff/fines":
                return "Fines Management";
            case "/staff/reports":
                return "Circulation Reports";
            case "/staff/reservations":
                return "Staff Reservations";
            case "/admin/dashboard":
                return "Admin Dashboard";
            case "/admin/tracking":
                return "Admin Tracking Dashboard";
            case "/admin/audit-logs":
                return "System Audit Log";
            case "/admin/settings":
                return "Library Settings";
            case "/admin/users":
                return "User Accounts";
            case "/admin/roles":
                return "Roles & Permissions";
            case "/admin/books":
                return "Book Management";
            case "/admin/categories":
                return "Category Management";
            case "/admin/authors":
                return "Author Management";
            case "/account":
            case "/account/profile":
                return "My Profile";
            case "/account/security":
                return "Account Security";
            default:
                break;
        }

        if (path.startsWith("/catalog/")) return "Book Details";
        if (path.startsWith("/member/fines/") && path.endsWith("/pay")) return "Fine Payment";
        if (path.startsWith("/member/fines/")) return "Fine Detail";
        if (path.startsWith("/member/")) return "Member Area";
        if (path.startsWith("/staff/loans/")) return "Loan Detail";
        if (path.startsWith("/staff/notifications/")) return "Email Notifications";
        if (path.startsWith("/staff/")) return "Staff Area";
        if (path.startsWith("/admin/books/")) return "Book Management";
        if (path.startsWith("/admin/authors/")) return "Author Management";
        if (path.startsWith("/admin/")) return "Admin Area";
        if (path.startsWith("/account/")) return "Account Settings";

        String[] segments = path.split("/");
        StringBuilder name = new StringBuilder();
        for (String seg : segments) {
            if (!seg.isEmpty()) {
                name.append(Character.toUpperCase(seg.charAt(0)))
                        .append(seg.substring(1))
                        .append(" ");
            }
        }
        String result = name.toString().trim();
        return result.isEmpty() ? path : result;
    }

    private static String truncate(String value, int maxLen) {
        if (value == null) return null;
        return value.length() > maxLen ? value.substring(0, maxLen) : value;
    }
}
