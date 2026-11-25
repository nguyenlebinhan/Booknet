package grp3.java.booknet.service.member;

import grp3.java.booknet.dto.member.MemberDirectoryView;
import grp3.java.booknet.entity.book.BookCopy;
import grp3.java.booknet.entity.book_service.Loan;
import grp3.java.booknet.enums.book_service.*;
import grp3.java.booknet.repository.book_service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/** Read-only integration of circulation and finalized fines for the member directory.
 * LoanItem is authoritative when present; old loans without items remain visible.
 * This service does not change loan states, calculate fines or record payments. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberActivityService {
    public static final Set<LoanItemStatus> ACTIVE_ITEMS = Set.of(
            LoanItemStatus.BORROWING, LoanItemStatus.BORROWED, LoanItemStatus.RETURN_REQUESTED);
    public static final Set<LoanStatus> ACTIVE_LOANS = Set.of(
            LoanStatus.BORROWING, LoanStatus.OVERDUE, LoanStatus.RENEW_PENDING, LoanStatus.RETURN_PENDING);
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private final LoanItemRepository items;
    private final LoanRepository loans;
    private final FineRepository fines;
    private final Clock clock;

    public record Summary(long onLoan, long overdue, BigDecimal outstanding) {
        public static final Summary EMPTY = new Summary(0, 0, BigDecimal.ZERO);
    }

    public Map<Long, Summary> summaries(Collection<Long> memberIds) {
        Map<Long, Summary> result = new HashMap<>();
        if (memberIds.isEmpty()) return result;
        LocalDate today = LocalDate.now(clock);
        var counts = new ArrayList<>(items.summarizeMembers(memberIds, ACTIVE_ITEMS, today));
        counts.addAll(loans.summarizeLegacyMembers(memberIds, ACTIVE_LOANS, today));
        for (var count : counts) {
            Summary previous = result.getOrDefault(count.getMemberId(), Summary.EMPTY);
            result.put(count.getMemberId(), new Summary(previous.onLoan() + count.getOnLoan(),
                    previous.overdue() + count.getOverdue(), previous.outstanding()));
        }
        for (var balance : fines.summarizeMembers(memberIds, FineStatus.UNPAID)) {
            Summary previous = result.getOrDefault(balance.getMemberId(), Summary.EMPTY);
            result.put(balance.getMemberId(), new Summary(previous.onLoan(), previous.overdue(), balance.getAmount()));
        }
        return result;
    }

    private record CurrentCopy(Loan loan, BookCopy copy, LocalDate due, boolean returnRequested) { }

    private List<CurrentCopy> currentCopies(Long memberId) {
        List<CurrentCopy> current = new ArrayList<>();
        for (var item : items.findByLoan_Member_IdAndStatusInAndReturnedAtIsNull(memberId, ACTIVE_ITEMS)) {
            current.add(new CurrentCopy(item.getLoan(), item.getCopy(), item.getDueDate(),
                    item.getStatus() == LoanItemStatus.RETURN_REQUESTED));
        }
        for (var loan : loans.findLegacyActiveByMember(memberId, ACTIVE_LOANS)) {
            current.add(new CurrentCopy(loan, loan.getCopy(), loan.getDueDate(), loan.getStatus() == LoanStatus.RETURN_PENDING));
        }
        return current;
    }

    public Set<Long> currentLoanIds(Long memberId) {
        return currentCopies(memberId).stream().map(c -> c.loan().getId()).collect(Collectors.toSet());
    }

    public List<MemberDirectoryView.Loan> currentLoans(Long memberId) {
        LocalDate today = LocalDate.now(clock);
        var current = currentCopies(memberId);
        return current.stream().sorted(Comparator.comparing(CurrentCopy::due)
                .thenComparing(c -> c.loan().getId()).thenComparing(c -> c.copy().getId()))
                .map(c -> {
                    var book = c.copy().getBook();
                    long lateDays = Math.max(0, ChronoUnit.DAYS.between(c.due(), today));
                    String note = lateDays > 0 ? lateDays + " day(s) overdue." : "Not yet due.";
                    if (c.returnRequested()) note += " Return requested; awaiting physical receipt.";
                    return new MemberDirectoryView.Loan(book.getTitle(),
                            book.getAuthors().stream().map(a -> a.getName()).sorted().collect(Collectors.joining(", ")),
                            c.copy().getBarcode(), "Loan #" + c.loan().getId(),
                            c.loan().getBorrowedAt() == null ? "\u2014" : c.loan().getBorrowedAt().toLocalDate().format(DATE),
                            c.due().format(DATE), lateDays > 0 ? "Overdue" : "Not yet due", note, book.getCoverUrl());
                }).toList();
    }

    public static String money(BigDecimal amount) {
        return NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN")).format(amount) + "\u0111";
    }
}
