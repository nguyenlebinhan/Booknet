package grp3.java.booknet.config.tracking;

import grp3.java.booknet.entity.tracking.UserLoginLog;
import grp3.java.booknet.security.dto.CustomUserDetails;
import grp3.java.booknet.security.principal.CustomOAuth2User;
import grp3.java.booknet.service.tracking.IpLocationResolver;
import grp3.java.booknet.service.tracking.TrackingService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.event.InteractiveAuthenticationSuccessEvent;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Listens to successful login events and records them in user_login_logs.
 * Uses Spring EventListener instead of modifying SecurityConfig handlers.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LoginSuccessListener {

    private final TrackingService trackingService;
    private final IpLocationResolver ipLocationResolver;

    @EventListener
    public void onLoginSuccess(InteractiveAuthenticationSuccessEvent event) {
        try {
            Authentication auth = event.getAuthentication();
            if (auth == null) {
                return;
            }

            // Extract user info
            String email = "unknown";
            String name = "Unknown";
            Object principal = auth.getPrincipal();
            if (principal instanceof CustomUserDetails u) {
                email = safeOr(u.getEmail(), auth.getName());
                name = safeOr(u.getFullName(), email);
            } else if (principal instanceof CustomOAuth2User u) {
                email = u.getEmail() != null ? u.getEmail() : auth.getName();
                name = safeOr(u.getFullName(), email);
            } else if (auth.getName() != null) {
                email = auth.getName();
                name = auth.getName();
            }

            // Determine login method
            String loginMethod;
            if (auth instanceof UsernamePasswordAuthenticationToken) {
                loginMethod = "PASSWORD";
            } else if (auth instanceof OAuth2AuthenticationToken) {
                loginMethod = "GOOGLE_OAUTH2";
            } else {
                loginMethod = "UNKNOWN";
            }

            // Extract role (first ROLE_ authority)
            String userRole = auth.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .filter(a -> a != null && a.startsWith("ROLE_"))
                    .map(a -> a.substring("ROLE_".length()))
                    .findFirst()
                    .orElse("MEMBER");

            String ipAddress = "127.0.0.1";
            String userAgent = "Unknown";
            String sessionId = null;

            if (auth.getDetails() instanceof WebAuthenticationDetails details && details.getRemoteAddress() != null) {
                ipAddress = details.getRemoteAddress();
            }

            try {
                if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
                    HttpServletRequest request = attrs.getRequest();
                    ipAddress = ipLocationResolver.extractIp(request);
                    String ua = request.getHeader("User-Agent");
                    if (ua != null && !ua.isBlank()) {
                        userAgent = ua;
                    }
                    if (request.getSession(false) != null) {
                        sessionId = request.getSession(false).getId();
                    }
                }
            } catch (Exception reqEx) {
                log.debug("Could not extract request details for login tracking: {}", reqEx.getMessage());
            }

            String location = ipLocationResolver.resolve(ipAddress);

            UserLoginLog loginLog = UserLoginLog.builder()
                    .userEmail(truncate(email, 150))
                    .userName(truncate(name, 150))
                    .userRole(truncate(userRole, 30))
                    .ipAddress(truncate(ipAddress, 60))
                    .location(truncate(location, 150))
                    .userAgent(truncate(userAgent, 500))
                    .loginMethod(truncate(loginMethod, 30))
                    .sessionId(truncate(sessionId, 100))
                    .build();

            trackingService.saveLoginLog(loginLog);

        } catch (Exception ex) {
            // Never let tracking break the login flow
            log.warn("Failed to record login event: {}", ex.getMessage());
        }
    }

    private static String safeOr(String value, String fallback) {
        return (value != null && !value.isBlank()) ? value : (fallback != null ? fallback : "unknown");
    }

    private static String truncate(String value, int maxLen) {
        if (value == null) return null;
        return value.length() > maxLen ? value.substring(0, maxLen) : value;
    }
}
