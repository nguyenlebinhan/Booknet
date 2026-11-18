package grp3.java.booknet.dto.member;

import grp3.java.booknet.entity.user.RoleDefinition;
import grp3.java.booknet.enums.user.RoleCapability;
import grp3.java.booknet.enums.user.UserRole;
import java.util.List;
import java.util.Set;
import static grp3.java.booknet.enums.user.RoleCapability.*;

/** Presentation groups share the same capabilities enforced by security. */
public record PermissionComparison(String label, Set<RoleCapability> required, String scope) {
    public String access(RoleDefinition role) {
        if (scope.equals("public")) return "Allowed";
        if (scope.equals("records")) {
            if (role.getSystemRole() == UserRole.ROLE_USER) return "Allowed";
            return role.getCapabilities().contains(MEMBER_READ) ? "Allowed" : "Not allowed";
        }
        return role.getCapabilities().containsAll(required) ? "Allowed" : "Not allowed";
    }
    public static List<PermissionComparison> rows() {
        return List.of(
            new PermissionComparison("Browse catalog & view books", Set.of(), "public"),
            new PermissionComparison("Edit personal profile", Set.of(PROFILE_UPDATE), "own"),
            new PermissionComparison("View loans, reservations & fines", Set.of(), "records"),
            new PermissionComparison("Request loans, reservations & renewals", Set.of(LOAN_REQUEST, RESERVE, RENEW), "own"),
            new PermissionComparison("Approve loans, renewals & returns", Set.of(CIRCULATION), "all"),
            new PermissionComparison("Create, edit & retire book records", Set.of(CATALOG_EDIT, COPY_MANAGE, BOOK_RETIRE), "all"),
            new PermissionComparison("View member service records", Set.of(MEMBER_READ), "all"),
            new PermissionComparison("View reports & send service emails", Set.of(INVENTORY_REPORT, CIRCULATION_REPORT, SERVICE_EMAIL), "all"),
            new PermissionComparison("Record finalized fine payments", Set.of(FINE_PAYMENT), "all"),
            new PermissionComparison("Manage accounts & lock access", Set.of(USER_ACCESS_UPDATE), "all"),
            new PermissionComparison("Create roles & edit permissions", Set.of(ROLE_MANAGE), "all"),
            new PermissionComparison("Change policies & waive fines", Set.of(POLICY_MANAGE), "all"),
            new PermissionComparison("View administrative audit log", Set.of(AUDIT_READ), "all")
        );
    }
}
