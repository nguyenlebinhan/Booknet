package grp3.java.booknet.enums.user;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.ArrayList;
import java.util.List;

/** Roles assigned to registered Booknet accounts and anonymous visitors. */
public enum UserRole {
    ROLE_GUEST,
    // Keep the persisted value for existing accounts; its business role is Member.
    ROLE_USER,
    ROLE_LIBRARIAN,
    ROLE_ADMIN;

    public String authorityName() {
        return this == ROLE_USER ? "ROLE_MEMBER" : name();
    }

    public List<GrantedAuthority> authorities() {
        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority(authorityName()));
        if (this == ROLE_LIBRARIAN || this == ROLE_ADMIN) {
            authorities.add(new SimpleGrantedAuthority(UserPermission.MEMBER_READ.name()));
            authorities.add(new SimpleGrantedAuthority(UserPermission.MEMBER_CARD_ACTIVATE.name()));
        }
        if (this == ROLE_ADMIN) {
            authorities.add(new SimpleGrantedAuthority(UserPermission.USER_READ.name()));
            authorities.add(new SimpleGrantedAuthority(UserPermission.USER_ROLE_UPDATE.name()));
            authorities.add(new SimpleGrantedAuthority(UserPermission.USER_ACCESS_UPDATE.name()));
        }
        return List.copyOf(authorities);
    }
}
