package grp3.java.booknet.service.member;

import grp3.java.booknet.dto.member.MemberDirectoryView.*;
import grp3.java.booknet.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberDirectoryService {
    private final UserRepository users;
    private final java.time.Clock clock;

    @PreAuthorize("hasAuthority('MEMBER_READ') and (!#admin or hasAuthority('USER_READ'))")
    public Directory directory(boolean admin, boolean demo, String keyword, String card, String loan, String fine, String accountStatus, int page) {
        if (demo) {
            List<Person> all = MemberPreviewData.people().stream().map(p -> new Person(p.id(), p.fullName(), p.email(), p.phone(), p.address(), p.role(), p.access(), p.verified(), p.memberCode(), p.cardNumber(), p.cardStatus(), p.issued(), p.expires(), p.onLoan(), p.overdue(), p.outstanding(), 0)).toList();
            String term = keyword.toLowerCase(Locale.ROOT);
            List<Person> filtered = all.stream()
                    .filter(p -> ((p.fullName() == null ? "" : p.fullName()) + " " + (p.email() == null ? "" : p.email())
                            + " " + (p.phone() == null ? "" : p.phone()) + " " + (p.memberCode() == null ? "" : p.memberCode())
                            + " " + (p.cardNumber() == null ? "" : p.cardNumber())).toLowerCase(Locale.ROOT).contains(term))
                    .filter(p -> card.isEmpty() || p.cardStatus().equals(card))
                    .filter(p -> accountStatus.isEmpty() || p.access().equals(accountStatus))
                    .filter(p -> loan.isEmpty() || (loan.equals("borrowed") ? p.onLoan() > 0
                            : loan.equals("overdue") ? p.overdue() > 0 : p.onLoan() == 0))
                    .filter(p -> fine.isEmpty() || (fine.equals("unpaid") ? !p.outstanding().equals("0đ") : p.outstanding().equals("0đ")))
                    .toList();
            int pages = Math.max(1, (filtered.size() + 6) / 7);
            int current = Math.min(Math.max(page, 0), pages - 1);
            return new Directory(filtered.subList(current * 7, Math.min(current * 7 + 7, filtered.size())),
                    filtered.size(), all.size(), all.stream().filter(p -> p.cardStatus().equals("Active")).count(),
                    all.stream().filter(p -> p.cardStatus().equals("Expired")).count(),
                    all.stream().filter(p -> p.onLoan() > 0).count(), all.stream().filter(p -> p.overdue() > 0).count(), current, pages);
        }
        if (!loan.isEmpty() || !fine.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Loan and fine filters are available in sample preview only.");
        }
        if (!admin && !accountStatus.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Account status filters are only available to administrators.");
        }
        var sort = Sort.by("fullName").ascending().and(Sort.by("id"));
        var result = users.findAll(criteria(admin, keyword, card, accountStatus), PageRequest.of(Math.max(0, page), 7, sort));
        int pages = Math.max(1, result.getTotalPages());
        if (page >= pages) result = users.findAll(criteria(admin, keyword, card, accountStatus), PageRequest.of(pages - 1, 7, sort));
        return new Directory(result.getContent().stream().map(u -> Person.from(u, java.time.LocalDate.now(clock))).toList(), result.getTotalElements(),
                users.count(criteria(admin, "", "", "")), users.count(criteria(admin, "", "Active", "")), users.count(criteria(admin, "", "Expired", "")),
                null, null, result.getNumber(), pages);
    }

    @PreAuthorize("hasAuthority('MEMBER_READ') and (!#admin or hasAuthority('USER_READ'))")
    public Person detail(boolean admin, boolean demo, Long id) {
        if (demo) return MemberPreviewData.people().stream().filter(p -> p.id().equals(id)).findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return users.findById(id).filter(user -> admin || (user.getRole() == grp3.java.booknet.enums.user.UserRole.ROLE_USER
                && (user.getAssignedRole() == null || user.getAssignedRole().getSystemRole() == grp3.java.booknet.enums.user.UserRole.ROLE_USER)))
                .map(u -> Person.from(u, java.time.LocalDate.now(clock))).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private org.springframework.data.jpa.domain.Specification<grp3.java.booknet.entity.user.User> criteria(
            boolean admin, String keyword, String status, String accountStatus) {
        return (root, query, cb) -> {
            var card = root.join("memberCard", jakarta.persistence.criteria.JoinType.LEFT);
            var role = root.join("assignedRole", jakarta.persistence.criteria.JoinType.LEFT);
            var memberScope = cb.and(cb.equal(root.get("role"), grp3.java.booknet.enums.user.UserRole.ROLE_USER),
                    cb.or(cb.isNull(role.get("id")), cb.equal(role.get("systemRole"), grp3.java.booknet.enums.user.UserRole.ROLE_USER)));
            var scope = admin ? cb.conjunction() : memberScope;
            String term = "%" + keyword.toLowerCase(Locale.ROOT).replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
            var search = cb.or(cb.like(cb.lower(root.get("fullName")), term, '!'),
                    cb.like(cb.lower(root.get("email")), term, '!'), cb.like(cb.lower(root.get("memberCode")), term, '!'),
                    cb.like(cb.lower(root.get("phone")), term, '!'),
                    cb.like(cb.lower(card.get("cardNumber")), term, '!'));
            java.time.LocalDate today = java.time.LocalDate.now(clock);
            jakarta.persistence.criteria.Predicate validity = switch (status) {
                case "Active" -> cb.and(cb.isNotNull(card.get("issuedOn")), cb.greaterThanOrEqualTo(card.get("expiresOn"), today));
                case "Expired" -> cb.and(cb.isNotNull(card.get("issuedOn")), cb.lessThan(card.get("expiresOn"), today));
                case "Pending" -> cb.and(memberScope, cb.isNull(card.get("issuedOn")));
                default -> cb.conjunction();
            };
            jakarta.persistence.criteria.Predicate account = accountStatus.isEmpty() ? cb.conjunction()
                    : cb.and(cb.equal(root.get("status"), grp3.java.booknet.enums.user.UserStatus.valueOf(accountStatus)),
                            accountStatus.equals("SUSPENDED") ? cb.conjunction() : cb.isTrue(root.get("enabled")));
            return cb.and(scope, search, validity, account);
        };
    }

    @PreAuthorize("hasAuthority('MEMBER_READ')")
    public List<Loan> previewLoans(boolean demo, Long id) {
        return demo ? MemberPreviewData.loans(id) : List.of();
    }
}
