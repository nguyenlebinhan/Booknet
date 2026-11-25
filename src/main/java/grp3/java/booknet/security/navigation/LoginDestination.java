package grp3.java.booknet.security.navigation;

import org.springframework.security.core.Authentication;

public final class LoginDestination {
    private LoginDestination() { }
    public static String forAuthentication(Authentication auth) {
        var roles = auth.getAuthorities().stream().map(a -> a.getAuthority()).toList();
        if (roles.contains("ROLE_ADMIN")) return "/admin/dashboard";
        if (roles.contains("ROLE_MEMBER")) return "/";
        if (roles.contains("ROLE_LIBRARIAN") || roles.contains("ROLE_CUSTOM")) {
            if (roles.contains("MEMBER_READ")) return "/staff/members";
            if (roles.contains("CIRCULATION")) return "/staff/loans";
            return "/account/profile";
        }
        return "/";
    }
}
