package grp3.java.booknet.security;

import grp3.java.booknet.enums.user.UserStatus;
import grp3.java.booknet.repository.user.UserRepository;
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
            boolean valid = users.findById(id).map(user -> user.isEnabled() && user.isEmailVerified()
                    && user.getStatus() == UserStatus.ACTIVE && user.getAuthVersion() == expectedVersion).orElse(false);
            if (!valid) {
                SecurityContextHolder.clearContext();
                if (request.getSession(false) != null) request.getSession(false).invalidate();
                Cookie cookie = new Cookie("remember-me", "");
                cookie.setPath(request.getContextPath().isEmpty() ? "/" : request.getContextPath());
                cookie.setHttpOnly(true);
                cookie.setSecure(request.isSecure());
                cookie.setMaxAge(0);
                response.addCookie(cookie);
                response.sendRedirect(request.getContextPath() + "/login?error=true");
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
