package grp3.java.booknet.service.impl.borrowing;

import grp3.java.booknet.dto.borrowing.MemberLoansView;
import grp3.java.booknet.dto.borrowing.ReturnDetailView;
import grp3.java.booknet.dto.borrowing.ReturnReceiptDto;
import grp3.java.booknet.entity.book.Author;
import grp3.java.booknet.entity.book.Book;
import grp3.java.booknet.entity.book_service.Loan;
import grp3.java.booknet.entity.book_service.LoanItem;
import grp3.java.booknet.entity.book_service.Reservation;
import grp3.java.booknet.entity.user.User;
import grp3.java.booknet.enums.book.CopyStatus;
import grp3.java.booknet.enums.book_service.LoanItemStatus;
import grp3.java.booknet.enums.book_service.LoanStatus;
import grp3.java.booknet.enums.book_service.ReservationStatus;
import grp3.java.booknet.enums.user.UserStatus;
import grp3.java.booknet.exception.account.AccountException;
import grp3.java.booknet.repository.book.BookCopyRepository;
import grp3.java.booknet.repository.book_service.LoanItemRepository;
import grp3.java.booknet.repository.book_service.LoanRepository;
import grp3.java.booknet.repository.book_service.ReservationRepository;
import grp3.java.booknet.repository.user.UserRepository;
import grp3.java.booknet.service.borrowing.MemberLoanService;
import grp3.java.booknet.service.fine.FineService;
import grp3.java.booknet.service.reservation.ReservationQueueService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemberLoanServiceImpl implements MemberLoanService {

    public static final int MAX_LOANS_LIMIT = 5;
    public static final int DUE_SOON_THRESHOLD_DAYS = 2;
    public static final int RENEW_DAYS = 7;
    public static final int MAX_RENEWS_ALLOWED = 1;

    private static final Set<LoanStatus> ACTIVE_LOAN_STATUSES = Set.of(
            LoanStatus.BORROWING,
            LoanStatus.RENEW_PENDING,
            LoanStatus.RETURN_PENDING,
            LoanStatus.OVERDUE
    );

    private static final Set<ReservationStatus> ACTIVE_RESERVATIONS = Set.of(
            ReservationStatus.PENDING,
            ReservationStatus.READY
    );

    private final UserRepository userRepository;
    private final LoanRepository loanRepository;
    private final LoanItemRepository loanItemRepository;
    private final ReservationRepository reservationRepository;
    private final ReservationQueueService reservationQueueService;
    private final BookCopyRepository bookCopyRepository;
    private final FineService fineService;

    @Override
    @Transactional
    public MemberLoansView getMemberLoansView(Long userId) {
        User user = validateAndGetUser(userId);
        LocalDate today = LocalDate.now();

        List<Loan> userLoans = loanRepository.findByMember_IdOrderByBorrowedAtDesc(userId);

        List<Loan> activeEntityLoans = new ArrayList<>();
        List<Loan> historyEntityLoans = new ArrayList<>();

        for (Loan loan : userLoans) {
            if ((loan.getStatus() == LoanStatus.BORROWING || loan.getStatus() == LoanStatus.RENEW_PENDING)
                    && loan.getDueDate().isBefore(today)) {
                loan.setStatus(LoanStatus.OVERDUE);
                loanRepository.save(loan);
            }

            if (ACTIVE_LOAN_STATUSES.contains(loan.getStatus())) {
                activeEntityLoans.add(loan);
            } else {
                historyEntityLoans.add(loan);
            }
        }

        int dueSoonCount = 0;
        int overdueCount = 0;
        String topAlertMessage = null;

        List<MemberLoansView.LoanItem> activeItems = new ArrayList<>();
        MemberLoansView.RenewSuggestion renewSuggestion = null;

        for (Loan loan : activeEntityLoans) {
            Book book = loan.getCopy().getBook();
            LocalDate dueDate = loan.getDueDate();
            long daysUntilDue = ChronoUnit.DAYS.between(today, dueDate);

            String dueBadgeText;
            String dueBadgeType;

            if (daysUntilDue < 0) {
                overdueCount++;
                long overdueDays = Math.abs(daysUntilDue);
                dueBadgeText = overdueDays + " days overdue";
                dueBadgeType = "overdue";
                if (topAlertMessage == null) {
                    topAlertMessage = book.getTitle() + " is " + overdueDays + " days overdue. The due date was "
                            + dueDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                            + ". Please return it to the circulation desk so the next reader doesn't have to wait.";
                }
            } else if (daysUntilDue <= DUE_SOON_THRESHOLD_DAYS) {
                dueSoonCount++;
                dueBadgeText = daysUntilDue == 0 ? "Due today" : daysUntilDue + " days left";
                dueBadgeType = "due-soon";
            } else {
                dueBadgeText = daysUntilDue + " days left";
                dueBadgeType = "normal";
            }

            boolean hasQueue = reservationQueueService.hasWaitingQueueForRenewal(book.getId(), userId);
            boolean renewalAllowed = user.effectiveAuthorities().stream().anyMatch(a -> a.getAuthority().equals("RENEW"));
            boolean canRenew = renewalAllowed && loan.getStatus() == LoanStatus.BORROWING
                    && loan.getRenewCount() < MAX_RENEWS_ALLOWED
                    && !dueDate.isBefore(today)
                    && !hasQueue;

            String renewalHint;
            if (loan.getStatus() == LoanStatus.RENEW_PENDING) {
                renewalHint = "Renewal requested - Waiting for librarian";
            } else if (loan.getStatus() == LoanStatus.RETURN_PENDING) {
                renewalHint = "Return requested - Please bring book to desk";
            } else if (loan.getStatus() == LoanStatus.OVERDUE) {
                renewalHint = "Please return soon";
            } else if (loan.getRenewCount() >= MAX_RENEWS_ALLOWED) {
                renewalHint = "Renew limit reached (1/1)";
            } else if (!renewalAllowed) {
                renewalHint = "Renewal permission is not enabled for your account";
            } else if (hasQueue) {
                renewalHint = "Cannot renew (Reserved by another reader)";
            } else {
                renewalHint = "Eligible for a 7-day renewal";
                if (renewSuggestion == null) {
                    renewSuggestion = new MemberLoansView.RenewSuggestion(
                            loan.getId(),
                            book.getTitle(),
                            dueDate.plusDays(RENEW_DAYS),
                            loan.getRenewCount(),
                            MAX_RENEWS_ALLOWED
                    );
                }
            }

            String authorNames = book.getAuthors().stream()
                    .map(Author::getName)
                    .collect(Collectors.joining(", "));
            if (authorNames.isBlank()) authorNames = "Unknown author";

            String cover = book.getCoverUrl() != null && !book.getCoverUrl().isBlank()
                    ? book.getCoverUrl()
                    : "/images/home/cover-hoa-vang.jpg";

            String loanCode = "LM-2026-" + String.format("%04d", loan.getId());

            activeItems.add(new MemberLoansView.LoanItem(
                    loan.getId(),
                    loanCode,
                    book.getId(),
                    book.getTitle(),
                    authorNames,
                    cover,
                    loan.getCopy().getBarcode(),
                    loan.getBorrowedAt() != null ? loan.getBorrowedAt().toLocalDate() : today,
                    dueDate,
                    loan.getStatus().name(),
                    dueBadgeText,
                    dueBadgeType,
                    loan.getStatus() != LoanStatus.RETURN_PENDING,
                    loan.getStatus() == LoanStatus.RETURN_PENDING,
                    loan.getReturnRequestedAt(),
                    canRenew,
                    loan.getStatus() == LoanStatus.RENEW_PENDING,
                    renewalHint
            ));
        }

        // History items
        List<MemberLoansView.HistoryItem> historyItems = historyEntityLoans.stream().map(loan -> {
            Book book = loan.getCopy().getBook();
            String loanCode = "LM-2026-" + String.format("%04d", loan.getId());
            String receiptCode = "REC-" + loan.getId() + "-" + (loan.getReturnedAt() != null
                    ? loan.getReturnedAt().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                    : today.format(DateTimeFormatter.BASIC_ISO_DATE));
            String librarian = loan.getReceivedBy() != null ? loan.getReceivedBy().getFullName() : "Desk Librarian";

            return new MemberLoansView.HistoryItem(
                    loan.getId(),
                    loanCode,
                    receiptCode,
                    book.getTitle(),
                    loan.getCopy().getBarcode(),
                    loan.getBorrowedAt() != null ? loan.getBorrowedAt().toLocalDate() : today,
                    loan.getDueDate(),
                    loan.getReturnRequestedAt(),
                    loan.getReturnedAt(),
                    librarian,
                    loan.getStatus().name()
            );
        }).toList();

        int booksReadThisYear = (int) historyEntityLoans.stream()
                .filter(l -> l.getReturnedAt() != null && l.getReturnedAt().getYear() == today.getYear())
                .count();

        String memberCode = user.getMemberCode() != null && !user.getMemberCode().isBlank()
                ? user.getMemberCode()
                : "TV-" + String.format("%05d", user.getId());

        List<Reservation> pendingReservationEntities =
                reservationRepository.findByMember_IdAndStatusIn(userId, ACTIVE_RESERVATIONS);

        List<MemberLoansView.ReservationItem> pendingReservations = pendingReservationEntities.stream()
                .map(res -> {
                    Book book = res.getBook();
                    String authorNames = book.getAuthors().stream()
                            .map(Author::getName)
                            .collect(Collectors.joining(", "));
                    if (authorNames.isBlank()) authorNames = "Unknown author";

                    String cover = book.getCoverUrl() != null && !book.getCoverUrl().isBlank()
                            ? book.getCoverUrl()
                            : "/images/home/cover-hoa-vang.jpg";

                    String resCode = "RQ-" + String.format("%04d", res.getId());

                    MemberLoansView.ReservationItem.ReservationStatus resStatus =
                            res.getStatus() == ReservationStatus.READY
                                    ? MemberLoansView.ReservationItem.ReservationStatus.READY
                                    : MemberLoansView.ReservationItem.ReservationStatus.PENDING;

                    return new MemberLoansView.ReservationItem(
                            res.getId(),
                            resCode,
                            book.getId(),
                            book.getTitle(),
                            authorNames,
                            cover,
                            res.getReservedAt(),
                            resStatus
                    );
                }).toList();

        return new MemberLoansView(
                user.getFullName(),
                memberCode,
                today,
                activeItems.size(),
                MAX_LOANS_LIMIT,
                dueSoonCount,
                overdueCount,
                booksReadThisYear,
                topAlertMessage,
                activeItems,
                pendingReservations,
                historyItems,
                renewSuggestion
        );
    }

    @Override
    @Transactional
    public void requestReturnBook(Long userId, Long loanId) {
        validateAndGetUser(userId);
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new AccountException(HttpStatus.NOT_FOUND, "Loan record not found."));

        if (!loan.getMember().getId().equals(userId)) {
            throw new AccountException(HttpStatus.FORBIDDEN, "Access denied: You do not own this loan.");
        }

        if (loan.getStatus() == LoanStatus.RETURNED || loan.getStatus() == LoanStatus.LOST) {
            throw new AccountException(HttpStatus.BAD_REQUEST, "Book has already been returned or marked lost.");
        }

        loan.setStatus(LoanStatus.RETURN_PENDING);
        loan.setReturnRequestedAt(LocalDateTime.now());
        loanRepository.save(loan);

        List<LoanItem> items = loanItemRepository.findByLoan_Id(loan.getId());
        for (LoanItem item : items) {
            if (item.getStatus() == LoanItemStatus.BORROWING || item.getStatus() == LoanItemStatus.BORROWED) {
                item.setStatus(LoanItemStatus.RETURN_REQUESTED);
                loanItemRepository.save(item);
            }
        }

        log.info("Member {} requested return for Loan ID {}", userId, loanId);
    }

    @Override
    @Transactional
    public void requestRenewLoan(Long userId, Long loanId) {
        User member = validateAndGetUser(userId);
        if (member.effectiveAuthorities().stream().noneMatch(a -> a.getAuthority().equals("RENEW"))) {
            throw new AccountException(HttpStatus.FORBIDDEN, "Your account does not have permission to request renewals.");
        }
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new AccountException(HttpStatus.NOT_FOUND, "Loan record not found."));

        if (!loan.getMember().getId().equals(userId)) {
            throw new AccountException(HttpStatus.FORBIDDEN, "Access denied: You do not own this loan.");
        }

        if (loan.getRenewCount() >= MAX_RENEWS_ALLOWED) {
            throw new AccountException(HttpStatus.BAD_REQUEST, "Maximum renewal limit reached (1/1).");
        }

        if (loan.getDueDate().isBefore(LocalDate.now())) {
            throw new AccountException(HttpStatus.BAD_REQUEST, "Cannot renew overdue loans. Please return the book to the library.");
        }

        if (reservationQueueService.hasWaitingQueueForRenewal(loan.getCopy().getBook().getId(), userId)) {
            throw new AccountException(HttpStatus.BAD_REQUEST, "Cannot renew this book because another reader has reserved it.");
        }

        loan.setStatus(LoanStatus.RENEW_PENDING);
        loan.setRenewRequestedAt(LocalDateTime.now());
        loanRepository.save(loan);
        log.info("Member {} requested renewal for Loan ID {}", userId, loanId);
    }

    @Override
    @Transactional(readOnly = true)
    public ReturnReceiptDto getReturnReceipt(Long userId, Long loanId) {
        validateAndGetUser(userId);
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new AccountException(HttpStatus.NOT_FOUND, "Loan record not found."));

        if (!loan.getMember().getId().equals(userId)) {
            throw new AccountException(HttpStatus.FORBIDDEN, "Access denied.");
        }

        if (loan.getStatus() != LoanStatus.RETURNED) {
            throw new AccountException(HttpStatus.BAD_REQUEST, "Receipt is only available once the return is confirmed by a librarian.");
        }

        String receiptCode = "REC-" + loan.getId() + "-" + (loan.getReturnedAt() != null
                ? loan.getReturnedAt().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                : LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE));
        String loanCode = "LM-2026-" + String.format("%04d", loan.getId());
        String librarian = loan.getReceivedBy() != null ? loan.getReceivedBy().getFullName() : "Desk Librarian";

        return new ReturnReceiptDto(
                receiptCode,
                loanCode,
                loan.getMember().getFullName(),
                loan.getMember().getMemberCode(),
                loan.getCopy().getBook().getTitle(),
                loan.getCopy().getBarcode(),
                loan.getBorrowedAt(),
                loan.getReturnedAt(),
                librarian
        );
    }

    private User validateAndGetUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AccountException(HttpStatus.UNAUTHORIZED, "User account not found."));
        if (!user.isEnabled() || !user.isEmailVerified() || user.getStatus() != UserStatus.ACTIVE) {
            throw new AccountException(HttpStatus.FORBIDDEN, "Your membership card is unverified, inactive or suspended.");
        }
        return user;
    }

    @Override
    @Transactional(readOnly = true)
    public ReturnDetailView getReturnDetail(Long requesterId, Long loanId, boolean isLibrarian) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new AccountException(HttpStatus.NOT_FOUND, "Loan record not found."));

        if (!isLibrarian && !loan.getMember().getId().equals(requesterId)) {
            throw new AccountException(HttpStatus.FORBIDDEN, "Access denied: This loan does not belong to you.");
        }

        Book book = loan.getCopy().getBook();
        LocalDate today = LocalDate.now();
        LocalDate dueDate = loan.getDueDate();
        long daysUntilDue = ChronoUnit.DAYS.between(today, dueDate);

        String dueBadgeText;
        String dueBadgeType;
        if (daysUntilDue < 0) {
            dueBadgeText = Math.abs(daysUntilDue) + " days overdue";
            dueBadgeType = "overdue";
        } else if (daysUntilDue <= DUE_SOON_THRESHOLD_DAYS) {
            dueBadgeText = daysUntilDue == 0 ? "Due today" : daysUntilDue + " days left";
            dueBadgeType = "due-soon";
        } else {
            dueBadgeText = "On time";
            dueBadgeType = "normal";
        }

        String authorNames = book.getAuthors().stream()
                .map(Author::getName)
                .collect(Collectors.joining(", "));
        if (authorNames.isBlank()) authorNames = "Unknown author";

        String cover = book.getCoverUrl() != null && !book.getCoverUrl().isBlank()
                ? book.getCoverUrl() : "/images/home/cover-hoa-vang.jpg";

        String loanCode = "LM-2026-" + String.format("%04d", loan.getId());

        boolean isReturned = loan.getStatus() == LoanStatus.RETURNED;
        String receiptCode = null;
        String librarianName = null;
        int booksStillOnLoan = 0;

        if (isReturned) {
            receiptCode = "TR-2026-" + String.format("%04d", loan.getId());
            librarianName = loan.getReceivedBy() != null ? loan.getReceivedBy().getFullName() : "Desk Librarian";
            booksStillOnLoan = (int) loanRepository.countByMember_IdAndStatusIn(
                    loan.getMember().getId(), ACTIVE_LOAN_STATUSES);
        }

        User member = loan.getMember();
        String memberCode = member.getMemberCode() != null && !member.getMemberCode().isBlank()
                ? member.getMemberCode()
                : "TV-" + String.format("%05d", member.getId());

        return new ReturnDetailView(
                loan.getId(),
                loanCode,
                book.getTitle(),
                authorNames,
                cover,
                loan.getCopy().getBarcode(),
                loan.getCopy().getBarcode(),
                loan.getBorrowedAt(),
                dueDate,
                loan.getReturnRequestedAt(),
                loan.getStatus().name(),
                dueBadgeText,
                dueBadgeType,
                member.getFullName(),
                memberCode,
                isReturned,
                receiptCode,
                loan.getReturnedAt(),
                librarianName,
                booksStillOnLoan,
                loan.getNote()
        );
    }

    @Override
    @Transactional
    public void confirmReturn(Long librarianId, Long loanId, String conditionNote) {
        User librarian = validateAndGetUser(librarianId);
        if (librarian.effectiveAuthorities().stream().noneMatch(a -> a.getAuthority().equals("CIRCULATION"))) {
            throw new AccountException(HttpStatus.FORBIDDEN, "Circulation permission is required.");
        }

        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new AccountException(HttpStatus.NOT_FOUND, "Loan record not found."));

        if (loan.getStatus() == LoanStatus.RETURNED) {
            throw new AccountException(HttpStatus.BAD_REQUEST, "This loan has already been confirmed as returned.");
        }
        if (loan.getStatus() == LoanStatus.LOST) {
            throw new AccountException(HttpStatus.BAD_REQUEST, "This loan is marked as lost.");
        }

        reservationQueueService.assignCopyToNextPendingReservation(loan.getCopy(), LocalDateTime.now());

        loan.setStatus(LoanStatus.RETURNED);
        loan.setReturnedAt(LocalDateTime.now());
        loan.setReceivedBy(librarian);
        if (conditionNote != null && !conditionNote.isBlank()) {
            loan.setNote(conditionNote.trim());
        }
        loanRepository.save(loan);

        // Nguoi 7 - UC29/UC20: Tao Fine neu tra tre
        fineService.finalizeFine(loan, librarian);

        log.info("Librarian {} confirmed return of Loan ID {} (copy: {})",
                librarian.getId(), loan.getId(), loan.getCopy().getBarcode());
    }
}
