package grp3.java.booknet.security.session;

import grp3.java.booknet.enums.user.UserStatus;
import grp3.java.booknet.repository.user.UserRepository;
import grp3.java.booknet.security.dto.CustomUserDetails;
import grp3.java.booknet.security.principal.CustomOAuth2User;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;
import java.util.stream.Collectors;

/** Re-check account state and revoke existing local AND Google sessions after password changes. */
@RequiredArgsConstructor
public class AccountSessionFilter extends OncePerRequestFilter {
    private final UserRepository users;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long id = null;
        long version = -1;
        if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails local) {
            id = local.getId();
            version = local.getAuthVersion();
        } else if (authentication != null && authentication.getPrincipal() instanceof CustomOAuth2User oauth) {
            id = oauth.getId();
            version = oauth.getAuthVersion();
        }
        if (id != null) {
            long expectedVersion = version;
            Set<String> sessionAuthorities = authentication.getAuthorities().stream()
                    .map(authority -> authority.getAuthority()).collect(Collectors.toSet());
            boolean valid = users.findById(id).map(user -> user.isEnabled() && user.isEmailVerified()
                    && user.getStatus() == UserStatus.ACTIVE && user.getAuthVersion() == expectedVersion
                    && user.effectiveAuthorities().stream().map(org.springframework.security.core.GrantedAuthority::getAuthority)
                            .collect(Collectors.toSet()).containsAll(sessionAuthorities.stream()
                                    .filter(authority -> !authority.startsWith("FACTOR_")).collect(Collectors.toSet()))
                    && sessionAuthorities.containsAll(user.effectiveAuthorities().stream()
                            .map(org.springframework.security.core.GrantedAuthority::getAuthority).collect(Collectors.toSet())))
                    .orElse(false);
            if (!valid) {
                SecurityContextHolder.clearContext();
                if (request.getSession(false) != null) request.getSession(false).invalidate();
                Cookie cookie = new Cookie("remember-me", "");
                cookie.setPath(request.getContextPath().isEmpty() ? "/" : request.getContextPath());
                cookie.setHttpOnly(true);
                cookie.setSecure(request.isSecure());
                cookie.setMaxAge(0);
                response.addCookie(cookie);
                String apiPath = request.getContextPath() + "/api";
                if (request.getRequestURI().equals(apiPath) || request.getRequestURI().startsWith(apiPath + "/")) {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"error\":\"Authentication required\"}");
                    return;
                }
                response.sendRedirect(request.getContextPath() + "/login?error=true");
                return;
            }
        }
        chain.doFilter(request, response);
    }

}
