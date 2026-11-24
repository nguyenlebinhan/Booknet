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
        var result=new ArrayList<GrantedAuthority>();result.add(new SimpleGrantedAuthority(authorityName()));
        defaults(this).stream().sorted().forEach(p -> result.add(new SimpleGrantedAuthority(p.name())));
        return List.copyOf(result);
    }
    public static java.util.Set<RoleCapability> defaults(UserRole role) {
        var set=java.util.EnumSet.noneOf(RoleCapability.class);
        if(role==ROLE_GUEST)return set;
        set.add(RoleCapability.PROFILE_UPDATE);
        if(role==ROLE_USER){set.addAll(java.util.Set.of(RoleCapability.LOAN_REQUEST,RoleCapability.RESERVE,RoleCapability.RENEW));return set;}
        set.addAll(java.util.Set.of(RoleCapability.CATALOG_EDIT,RoleCapability.COPY_MANAGE,RoleCapability.BOOK_RETIRE,RoleCapability.MEMBER_READ,RoleCapability.MEMBER_CARD_ACTIVATE,RoleCapability.CIRCULATION,RoleCapability.FINE_PAYMENT,RoleCapability.INVENTORY_REPORT,RoleCapability.CIRCULATION_REPORT,RoleCapability.SERVICE_EMAIL));
        if(role==ROLE_ADMIN)for(var p:RoleCapability.values())if(p.administrative())set.add(p);
        return set;
    }
}
