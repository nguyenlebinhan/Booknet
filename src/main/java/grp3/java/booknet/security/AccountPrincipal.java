package grp3.java.booknet.security;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;

public final class AccountPrincipal {
    private AccountPrincipal() { }

    public static Long id(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            throw new InsufficientAuthenticationException("Please sign in.");
        }
        if (authentication.getPrincipal() instanceof CustomUserDetails local) return local.getId();
        if (authentication.getPrincipal() instanceof CustomOAuth2User oauth) return oauth.getId();
        throw new InsufficientAuthenticationException("Please sign in again.");
    }
}
