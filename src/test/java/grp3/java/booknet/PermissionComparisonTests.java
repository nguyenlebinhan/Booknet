package grp3.java.booknet;

import grp3.java.booknet.dto.member.PermissionComparison;
import grp3.java.booknet.entity.user.RoleDefinition;
import grp3.java.booknet.enums.user.RoleCapability;
import grp3.java.booknet.enums.user.UserRole;
import org.junit.jupiter.api.Test;
import java.util.HashSet;
import java.util.Set;
import static grp3.java.booknet.enums.user.RoleCapability.*;
import static org.assertj.core.api.Assertions.assertThat;

class PermissionComparisonTests {
    private RoleDefinition role(UserRole type, RoleCapability... capabilities) {
        var role = new RoleDefinition();
        role.setSystemRole(type);
        role.setCapabilities(new HashSet<>(Set.of(capabilities)));
        return role;
    }

    private String access(String label, RoleDefinition role) {
        return PermissionComparison.rows().stream().filter(row -> row.label().equals(label))
                .findFirst().orElseThrow().access(role);
    }

    @Test void publicAndPersonalAccessDoNotRequireStaffCapabilities() {
        var guest = role(UserRole.ROLE_GUEST);
        var member = role(UserRole.ROLE_USER);
        assertThat(access("Browse catalog & view published books", guest)).isEqualTo("Allowed");
        assertThat(access("View own account & membership card", guest)).isEqualTo("Not allowed");
        assertThat(access("View own loans & return history", member)).isEqualTo("Allowed");
        assertThat(access("View own fines & payment instructions", member)).isEqualTo("Allowed");
        assertThat(access("Update own profile", member)).isEqualTo("Not allowed");
        assertThat(access("Place or cancel a reservation", member)).isEqualTo("Not allowed");
        assertThat(access("View own reservations", member)).isEqualTo("Allowed");
        assertThat(access("Update own profile", role(null, USER_ACCESS_UPDATE))).isEqualTo("Allowed");
    }

    @Test void staffReportsAndDeskPaymentsFollowRoleAndUrlGuards() {
        var librarian = role(UserRole.ROLE_LIBRARIAN, MEMBER_READ);
        assertThat(access("View circulation reports", librarian)).isEqualTo("Allowed");
        assertThat(access("View fines & collect payment at the desk", librarian)).isEqualTo("Allowed");
        var custom = role(null, MEMBER_READ, INVENTORY_REPORT, CIRCULATION_REPORT, FINE_PAYMENT);
        assertThat(access("View circulation reports", custom)).isEqualTo("Not allowed");
        assertThat(access("View fines & collect payment at the desk", custom)).isEqualTo("Not allowed");
        assertThat(access("View circulation reports", role(UserRole.ROLE_LIBRARIAN, CIRCULATION_REPORT))).isEqualTo("Not allowed");
        assertThat(access("Compose & send email notifications", role(null, SERVICE_EMAIL))).isEqualTo("Allowed");
    }

    @Test void roleBoundPagesCannotBeGrantedByCapabilitiesAlone() {
        var custom = role(null, USER_READ, USER_ACCESS_UPDATE, ROLE_MANAGE, MEMBER_READ, RESERVE, RENEW);
        assertThat(access("View user accounts", custom)).isEqualTo("Not allowed");
        assertThat(access("Compare roles & permissions", custom)).isEqualTo("Not allowed");
        assertThat(access("Place or cancel a reservation", custom)).isEqualTo("Not allowed");
        assertThat(access("View user accounts", role(UserRole.ROLE_ADMIN, USER_READ))).isEqualTo("Not allowed");
        assertThat(access("View user accounts", role(UserRole.ROLE_ADMIN, USER_READ, MEMBER_READ))).isEqualTo("Allowed");
        assertThat(access("Configure circulation policies", role(UserRole.ROLE_ADMIN))).isEqualTo("Allowed");
        assertThat(access("Manage categories", role(UserRole.ROLE_LIBRARIAN))).isEqualTo("Allowed");
    }

    @Test void independentActionsAreNotHiddenBehindGroupedPermissions() {
        var custom = role(null, CATALOG_EDIT);
        assertThat(access("Create & edit book records", custom)).isEqualTo("Allowed");
        assertThat(access("Manage physical copies", custom)).isEqualTo("Not allowed");
        assertThat(access("Discontinue or delete book records", custom)).isEqualTo("Not allowed");
        assertThat(access("Activate a member card", role(null, MEMBER_CARD_ACTIVATE))).isEqualTo("Not allowed");
        assertThat(access("Activate a member card", role(null, MEMBER_READ, MEMBER_CARD_ACTIVATE))).isEqualTo("Allowed");
        assertThat(PermissionComparison.rows()).noneMatch(row -> row.label().contains("waive") || row.label().contains("edit permissions"));
    }
}
