package grp3.java.booknet.dto.member;

import grp3.java.booknet.entity.user.User;

import java.util.List;

/** Read-only presentation data. Null card/loan/fine values mean not connected, never zero. */
public final class MemberDirectoryView {
    private MemberDirectoryView() { }

    public record Person(Long id, String fullName, String email, String phone, String address,
                         String role, String access, boolean verified, String memberCode,
                         String cardNumber, String cardStatus, String issued, String expires,
                         Long onLoan, Long overdue, String outstanding, long revision) {
        public static Person from(User user) {
            return from(user, java.time.LocalDate.now(java.time.Clock.systemUTC()));
        }
        public static Person from(User user, java.time.LocalDate today) {
            String role = user.roleLabel();
            var card = user.getMemberCard();
            var dateFormat = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy");
            return new Person(user.getId(), user.getFullName(), user.getEmail(), user.getPhone(),
                    user.getAddress(), role, user.getStatus() == grp3.java.booknet.enums.user.UserStatus.EXPIRED
                            ? "EXPIRED" : user.getStatus() == grp3.java.booknet.enums.user.UserStatus.SUSPENDED || !user.isEnabled()
                                    ? "SUSPENDED" : "ACTIVE",
                    user.isEmailVerified(), user.getMemberCode(), card == null ? null : card.getCardNumber(),
                    card == null ? (user.getRole() == grp3.java.booknet.enums.user.UserRole.ROLE_USER && (user.getAssignedRole()==null || user.getAssignedRole().getSystemRole()==grp3.java.booknet.enums.user.UserRole.ROLE_USER) ? "Pending" : "Not applicable") : card.status(today),
                    card == null || card.getIssuedOn() == null ? null : card.getIssuedOn().format(dateFormat),
                    card == null || card.getExpiresOn() == null ? null : card.getExpiresOn().format(dateFormat),
                    null, null, null, user.getAuthVersion());
        }

        public String initials() {
            String[] words = fullName.trim().split("\\s+");
            return (words[0].substring(0, 1) + (words.length > 1 ? words[words.length - 1].substring(0, 1) : ""))
                    .toUpperCase(java.util.Locale.ROOT);
        }

        public String cardTone() {
            return switch (cardStatus) {
                case "Active" -> "positive";
                case "Expired" -> "warning";
                case "Pending" -> "neutral";
                default -> "muted";
            };
        }
    }

    public record Loan(String title, String author, String copy, String loanId, String borrowed,
                       String due, String state, String note, String cover) { }

    public record Directory(List<Person> people, long total, long registered, Long active, Long expired,
                            Long withLoans, Long withOverdue, int page, int pages) {
        public long first() { return total == 0 ? 0 : (long) page * 7 + 1; }
        public long last() { return total == 0 ? 0 : first() + people.size() - 1; }
    }
}
