package grp3.java.booknet.service.impl.admin;

import grp3.java.booknet.dto.admin.LibrarianDashboardSummary;
import grp3.java.booknet.dto.admin.RecentActivityItem;
import grp3.java.booknet.dto.admin.ReportSummary;
import grp3.java.booknet.entity.AuditLog;
import grp3.java.booknet.entity.book_service.Loan;
import grp3.java.booknet.enums.book.CopyStatus;
import grp3.java.booknet.enums.book.PublicationStatus;
import grp3.java.booknet.enums.book_service.FineStatus;
import grp3.java.booknet.enums.book_service.LoanStatus;
import grp3.java.booknet.enums.book_service.ReservationStatus;
import grp3.java.booknet.enums.user.UserRole;
import grp3.java.booknet.repository.AuditLogRepository;
import grp3.java.booknet.repository.book.BookCopyRepository;
import grp3.java.booknet.repository.book.BookRepository;
import grp3.java.booknet.repository.book_service.FineRepository;
import grp3.java.booknet.repository.book_service.LoanRepository;
import grp3.java.booknet.repository.book_service.ReservationRepository;
import grp3.java.booknet.repository.user.UserRepository;
import grp3.java.booknet.service.admin.StaffDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class StaffDashboardServiceImpl implements StaffDashboardService {

    private final BookRepository bookRepository;
    private final BookCopyRepository bookCopyRepository;
    private final LoanRepository loanRepository;
    private final ReservationRepository reservationRepository;
    private final UserRepository userRepository;
    private final FineRepository fineRepository;
    private final AuditLogRepository auditLogRepository;

    // --- Librarian Dashboard Summary ---

    @Override
    @Transactional(readOnly = true)
    public LibrarianDashboardSummary getLibrarianSummary() {
        LocalDate today = LocalDate.now();

        long publishedTitles  = bookRepository.countByDeletedFalseAndPublicationStatus(PublicationStatus.PUBLISHED);
        long totalCopies      = bookCopyRepository.count();
        long borrowedCopies   = bookCopyRepository.countByStatus(CopyStatus.BORROWED);
        long availableCopies  = bookCopyRepository.countByStatus(CopyStatus.AVAILABLE);
        long overdueLoans     = loanRepository.countByDueDateBeforeAndStatusIn(
                today, List.of(LoanStatus.BORROWING, LoanStatus.OVERDUE));
        long pendingRes       = reservationRepository.countByStatus(ReservationStatus.PENDING);
        long awaitingHandover = reservationRepository.countByStatus(ReservationStatus.READY);
        long pendingReturns   = loanRepository.countByStatus(LoanStatus.RETURN_PENDING);
        long unpaidFines      = fineRepository.countByStatus(FineStatus.UNPAID);

        return new LibrarianDashboardSummary(
                publishedTitles, totalCopies, borrowedCopies, availableCopies,
                overdueLoans, pendingRes,
                awaitingHandover, pendingReturns, overdueLoans, unpaidFines
        );
    }

    // --- Recent Activity ---

    @Override
    @Transactional(readOnly = true)
    public List<RecentActivityItem> getRecentLoanActivity() {
        List<Loan> recentLoans = loanRepository.findTop10ByOrderByUpdatedAtDesc();
        List<RecentActivityItem> items = new ArrayList<>();

        for (Loan loan : recentLoans) {
            String action;
            String description;
            String actorEmail = loan.getMember() != null ? loan.getMember().getEmail() : "system";

            String bookTitle = (loan.getCopy() != null && loan.getCopy().getBook() != null)
                    ? loan.getCopy().getBook().getTitle()
                    : "Unknown book";

            if (loan.getStatus() == LoanStatus.RETURNED) {
                action      = "RETURN_CONFIRMED";
                description = "Return confirmed: '" + bookTitle + "' (Loan #" + loan.getId() + ")";
            } else if (loan.getStatus() == LoanStatus.BORROWING) {
                action      = "LOAN_ISSUED";
                description = "Book issued: '" + bookTitle + "' to " + actorEmail;
            } else if (loan.getStatus() == LoanStatus.OVERDUE) {
                action      = "LOAN_OVERDUE";
                description = "Overdue: '" + bookTitle + "' (" + actorEmail + ")";
            } else {
                action      = loan.getStatus().name();
                description = "Loan #" + loan.getId() + " status: " + loan.getStatus().name();
            }

            LocalDateTime ts = loan.getUpdatedAt() != null
                    ? loan.getUpdatedAt()
                    : (loan.getBorrowedAt() != null ? loan.getBorrowedAt() : LocalDateTime.now());

            items.add(new RecentActivityItem(
                    action, description, actorEmail, "MEMBER",
                    "Loan", loan.getId(), ts
            ));
        }
        return items;
    }

    @Override
    @Transactional(readOnly = true)
    public List<RecentActivityItem> getRecentAdminActivity() {
        List<AuditLog> logs = auditLogRepository.findTop10ByOrderByOccurredAtDesc();
        return logs.stream().map(log -> new RecentActivityItem(
                log.getAction(),
                log.getDescription(),
                log.getActorEmail(),
                log.getActorRole(),
                log.getTargetType(),
                log.getTargetId(),
                log.getOccurredAt()
        )).toList();
    }

    // --- Reports ---

    @Override
    @Transactional(readOnly = true)
    public ReportSummary getReport(LocalDate from, LocalDate to) {
        LocalDateTime fromDt = from.atStartOfDay();
        LocalDateTime toDt   = to.atTime(23, 59, 59);

        long totalLoaned   = loanRepository.countByBorrowedAtBetween(fromDt, toDt);
        List<Loan> returnedLoans = loanRepository.findByReturnedAtBetween(fromDt, toDt);
        long totalReturned = returnedLoans.size();

        long onTimeReturns = 0;
        long grp1 = 0, grp2 = 0, grp3 = 0;

        for (Loan l : returnedLoans) {
            if (l.getReturnedAt() != null && l.getDueDate() != null) {
                LocalDate retDate = l.getReturnedAt().toLocalDate();
                if (!retDate.isAfter(l.getDueDate())) {
                    onTimeReturns++;
                } else {
                    long days = ChronoUnit.DAYS.between(l.getDueDate(), retDate);
                    if      (days <= 3) grp1++;
                    else if (days <= 7) grp2++;
                    else                grp3++;
                }
            } else {
                onTimeReturns++;
            }
        }
        long lateReturns = totalReturned - onTimeReturns;

        // Top 5 titles
        List<Object[]> top5Raw = loanRepository.findTopBorrowedTitles(fromDt, toDt, PageRequest.of(0, 5));
        List<ReportSummary.TitleStat> top5 = top5Raw.stream()
                .map(row -> new ReportSummary.TitleStat((String) row[0], (Long) row[1]))
                .toList();

        // Fine totals (all-time)
        BigDecimal finePaid      = fineRepository.sumAmountByStatus(FineStatus.PAID);
        BigDecimal fineUnpaid    = fineRepository.sumAmountByStatus(FineStatus.UNPAID);
        BigDecimal fineEstimated = fineRepository.sumAmountByStatus(FineStatus.ESTIMATED);

        return new ReportSummary(
                from.toString(), to.toString(),
                totalLoaned, totalReturned, onTimeReturns, lateReturns,
                grp1, grp2, grp3,
                top5,
                finePaid, fineUnpaid, fineEstimated
        );
    }

    // --- Role distribution ---

    @Override
    @Transactional(readOnly = true)
    public Map<String, Long> getRoleDistribution() {
        List<Object[]> rows = userRepository.countByRoleGrouped();
        Map<String, Long> result = new LinkedHashMap<>();
        for (Object[] row : rows) {
            UserRole role  = (UserRole) row[0];
            Long     count = (Long) row[1];
            String   label = switch (role) {
                case ROLE_ADMIN     -> "Admin";
                case ROLE_LIBRARIAN -> "Librarian";
                case ROLE_USER      -> "Member";
                default             -> "Guest";
            };
            result.merge(label, count, Long::sum);
        }
        return result;
    }
}
