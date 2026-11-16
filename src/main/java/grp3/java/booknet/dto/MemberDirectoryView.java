package grp3.java.booknet.dto;

import grp3.java.booknet.entity.user.User;

import java.util.List;

/** Read-only presentation data. Null card/loan/fine values mean not connected, never zero. */
public final class MemberDirectoryView {
    private MemberDirectoryView() { }

    public record Person(Long id, String fullName, String email, String phone, String address,
                         String role, String access, boolean verified, String memberCode,
                         String cardNumber, String cardStatus, String issued, String expires,
                         Long onLoan, Long overdue, String outstanding) {
        public static Person from(User user) {
            String role = switch (user.getRole()) {
                case ROLE_USER -> "Member";
                case ROLE_LIBRARIAN -> "Librarian";
                case ROLE_ADMIN -> "Admin";
                case ROLE_GUEST -> "Guest";
            };
            return new Person(user.getId(), user.getFullName(), user.getEmail(), user.getPhone(),
                    user.getAddress(), role, !user.isEnabled() ? "Disabled" : user.getStatus().name(),
                    user.isEmailVerified(), user.getMemberCode(), null, "Not connected", null, null,
                    null, null, null);
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
