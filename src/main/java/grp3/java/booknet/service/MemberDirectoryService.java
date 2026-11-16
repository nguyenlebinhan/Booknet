package grp3.java.booknet.service;

import grp3.java.booknet.dto.MemberDirectoryView.*;
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

    @PreAuthorize("hasAuthority('MEMBER_READ') and (!#admin or hasAuthority('USER_READ'))")
    public Directory directory(boolean admin, boolean demo, String keyword, String card, String loan, String fine, int page) {
        if (demo) {
            List<Person> all = MemberPreviewData.people();
            String term = keyword.toLowerCase(Locale.ROOT);
            List<Person> filtered = all.stream()
                    .filter(p -> (p.fullName() + " " + p.email() + " " + p.cardNumber()).toLowerCase(Locale.ROOT).contains(term))
                    .filter(p -> card.isEmpty() || p.cardStatus().equals(card))
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
        // Card/circulation filters must not silently pretend to work before their backends exist.
        if (!card.isEmpty() || !loan.isEmpty() || !fine.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "These filters are available in sample preview only.");
        }
        var sort = Sort.by("fullName").ascending().and(Sort.by("id"));
        var result = users.searchDirectory(admin, keyword, PageRequest.of(Math.max(0, page), 7, sort));
        int pages = Math.max(1, result.getTotalPages());
        if (page >= pages) result = users.searchDirectory(admin, keyword, PageRequest.of(pages - 1, 7, sort));
        return new Directory(result.getContent().stream().map(Person::from).toList(), result.getTotalElements(),
                users.countDirectory(admin), null, null, null, null, result.getNumber(), pages);
    }

    @PreAuthorize("hasAuthority('MEMBER_READ') and (!#admin or hasAuthority('USER_READ'))")
    public Person detail(boolean admin, boolean demo, Long id) {
        if (demo) return MemberPreviewData.people().stream().filter(p -> p.id().equals(id)).findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return users.findById(id).filter(user -> admin || user.getRole() == grp3.java.booknet.enums.user.UserRole.ROLE_USER)
                .map(Person::from).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @PreAuthorize("hasAuthority('MEMBER_READ')")
    public List<Loan> previewLoans(boolean demo, Long id) {
        return demo ? MemberPreviewData.loans(id) : List.of();
    }
}
