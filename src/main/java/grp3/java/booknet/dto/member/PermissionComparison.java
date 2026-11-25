package grp3.java.booknet.dto.member;

import grp3.java.booknet.entity.user.RoleDefinition;
import grp3.java.booknet.enums.user.RoleCapability;
import java.util.List;
import java.util.Set;
import static grp3.java.booknet.enums.user.RoleCapability.*;

/** Read-only action matrix. Rules include URL guards and controller/service checks,
 * rather than treating every stored capability as access to an implemented page. */
public record PermissionComparison(String label, String detail, Set<String> allowedRoles,
                                   Set<RoleCapability> required, boolean anyCapability) {
    private static final Set<String> ACCOUNTS = Set.of("ROLE_MEMBER", "ROLE_LIBRARIAN", "ROLE_ADMIN", "ROLE_CUSTOM");
    private static final Set<String> MEMBER = Set.of("ROLE_MEMBER");
    private static final Set<String> STAFF = Set.of("ROLE_LIBRARIAN", "ROLE_ADMIN");
    private static final Set<String> ADMIN = Set.of("ROLE_ADMIN");

    public String access(RoleDefinition role) {
        String authority = role.getSystemRole() == null ? "ROLE_CUSTOM" : role.getSystemRole().authorityName();
        if (!allowedRoles.isEmpty() && !allowedRoles.contains(authority)) return "Not allowed";
        boolean enabled = anyCapability
                ? required.stream().anyMatch(role.getCapabilities()::contains)
                : role.getCapabilities().containsAll(required);
        return enabled ? "Allowed" : "Not allowed";
    }

    private static PermissionComparison action(String label, String detail, Set<String> roles, RoleCapability... caps) {
        return new PermissionComparison(label, detail, roles, Set.of(caps), false);
    }

    public static List<PermissionComparison> rows() {
        return List.of(
            // SecurityConfig public routes; no account role or capability is required.
            action("Browse catalog & view published books", "Public catalog and book details; unpublished books are restricted.", Set.of()),
            // /account/** and AccountServiceImpl.updateProfile (either authority).
            action("View own account & membership card", "Only the signed-in account's profile and card.", ACCOUNTS),
            new PermissionComparison("Update own profile", "Update own name, phone, address and avatar; email and role stay fixed.", ACCOUNTS, Set.of(PROFILE_UPDATE, USER_ACCESS_UPDATE), true),
            action("Manage own password & Google connection", "Own account only; password, verification and linked-identity checks still apply.", ACCOUNTS),
            // /member/** is MEMBER-only even where a method lists additional roles.
            action("View own loans & return history", "My books and return receipts belonging to the signed-in member.", MEMBER),
            action("Request a book return", "Request only; the librarian must physically receive the copy.", MEMBER),
            // BorrowController and BorrowServiceImpl.validateAndGetUser.
            action("Submit a borrowing request", "An eligible, active membership card and available books are required.", Set.of("ROLE_MEMBER", "ROLE_ADMIN", "ROLE_CUSTOM"), LOAN_REQUEST),
            action("Request a loan renewal", "Own eligible loans only; library renewal rules still apply.", MEMBER, RENEW),
            action("View own reservations", "Only reservations belonging to the signed-in member.", MEMBER),
            action("Place or cancel a reservation", "Own reservations only; availability and reservation limits still apply.", MEMBER, RESERVE),
            action("View own fines & payment instructions", "Own fines only; payment instructions are available for unpaid fines.", MEMBER),
            // AdminBookController, AdminBookCopyController and the explicit book URL guard.
            action("Create & edit book records", "Manage catalog metadata and publication information.", ACCOUNTS, CATALOG_EDIT),
            action("Manage physical copies", "Manage individual book copies and their availability.", ACCOUNTS, COPY_MANAGE),
            action("Discontinue or delete book records", "Deletion is subject to circulation and history checks.", ACCOUNTS, BOOK_RETIRE),
            action("Manage categories", "Category administration is restricted to Librarian and Administrator roles.", STAFF),
            action("Manage authors", "Author administration is available to Librarian and Administrator roles.", STAFF),
            // Circulation/reservation aliases have explicit CIRCULATION URL guards.
            action("Manage loans, renewals & returns", "Staff circulation actions; loan status and eligibility rules still apply.", ACCOUNTS, CIRCULATION),
            action("Manage reservation queue & pickups", "Allocate copies and process pickup or cancellation through staff reservation actions.", ACCOUNTS, CIRCULATION),
            // Generic /staff/** requires MEMBER_READ, followed by the card action guard.
            action("Search & view member profiles", "Member identity, contact details and card data for library service. Loan/fine details in this directory are not connected yet.", ACCOUNTS, MEMBER_READ),
            action("Activate a member card", "Requires verified email, identity confirmation and a valid future expiry date.", ACCOUNTS, MEMBER_READ, MEMBER_CARD_ACTIVATE),
            // StaffReportsController and StaffFinesController use staff roles, not report/payment capabilities.
            action("View circulation reports", "Staff reports covering borrowing activity in the selected date range.", STAFF, MEMBER_READ),
            action("View fines & collect payment at the desk", "Staff fine ledger and confirmation of payment for an unpaid fine.", STAFF, MEMBER_READ),
            action("Compose & send email notifications", "Load/reset templates, autosave wording, preview recipients and explicitly confirm dispatch.", ACCOUNTS, SERVICE_EMAIL),
            // /admin/** requires ADMIN in addition to directory/service capabilities.
            action("View user accounts", "Member and staff account records in User accounts.", ADMIN, MEMBER_READ, USER_READ),
            action("Create an account & send an invitation", "Choose an existing role at creation; the account role cannot be changed afterwards.", ADMIN, USER_ACCESS_UPDATE),
            action("Update account status & access", "Manage eligible Member, Librarian or custom accounts; administrator and self changes are restricted.", ADMIN, USER_ACCESS_UPDATE),
            action("Edit a user's profile", "Update allowed profile fields on an eligible account; role and email stay fixed.", ADMIN, USER_ACCESS_UPDATE),
            action("Reset access or resend an invitation", "Password-reset email, session sign-out and invitation resend require a reason, confirmation and account checks.", ADMIN, USER_ACCESS_UPDATE),
            action("Compare roles & permissions", "Read-only comparison; creating roles and editing permissions are disabled.", ADMIN),
            action("Configure circulation policies", "Configure borrowing limits, loan/renewal periods, fine rate and reservation hold duration.", ADMIN)
        );
    }
}
