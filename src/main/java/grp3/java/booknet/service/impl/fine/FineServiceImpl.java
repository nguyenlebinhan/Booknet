package grp3.java.booknet.service.impl.fine;

import grp3.java.booknet.dto.member.MemberFineDto;
import grp3.java.booknet.entity.book.Author;
import grp3.java.booknet.entity.book.Book;
import grp3.java.booknet.entity.book_service.Fine;
import grp3.java.booknet.entity.book_service.Loan;
import grp3.java.booknet.entity.user.User;
import grp3.java.booknet.enums.book_service.FineReason;
import grp3.java.booknet.enums.book_service.FineStatus;
import grp3.java.booknet.enums.book_service.LoanStatus;
import grp3.java.booknet.exception.account.AccountException;
import grp3.java.booknet.repository.book_service.FineRepository;
import grp3.java.booknet.repository.book_service.LoanRepository;
import grp3.java.booknet.service.admin.CirculationPolicyService;
import grp3.java.booknet.service.fine.FineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FineServiceImpl implements FineService {

    private final FineRepository fineRepository;
    private final CirculationPolicyService policyService;
    private final LoanRepository loanRepository;

    // ─────────────────────────────────────────────────────────────────────────
    // Tính phí ước tính (KHÔNG ghi DB)
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public BigDecimal calculateEstimatedFine(Loan loan) {
        if (loan == null || loan.getDueDate() == null) return BigDecimal.ZERO;

        LocalDate today = LocalDate.now();
        LocalDate dueDate = loan.getDueDate();

        // Chỉ tính khi quá hạn (và chưa trả)
        long overdueDays = ChronoUnit.DAYS.between(dueDate, today);
        if (overdueDays <= 0) return BigDecimal.ZERO;

        BigDecimal dailyRate = policyService.getPolicy().getDailyFineRate();
        return dailyRate.multiply(BigDecimal.valueOf(overdueDays));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Chốt phí khi Librarian nhận sách (gọi từ confirmReturn)
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public Optional<Fine> finalizeFine(Loan loan, User librarian) {
        if (loan == null || loan.getDueDate() == null || loan.getReturnedAt() == null) {
            return Optional.empty();
        }

        LocalDate dueDate = loan.getDueDate();
        LocalDate returnedDate = loan.getReturnedAt().toLocalDate();

        // Số ngày quá hạn = ngày trả thực tế - ngày hết hạn (tính từ dueDate+1)
        long overdueDays = ChronoUnit.DAYS.between(dueDate, returnedDate);
        if (overdueDays <= 0) {
            log.debug("Loan {} returned on time (dueDate={}, returnedAt={}) — no fine.",
                    loan.getId(), dueDate, returnedDate);
            return Optional.empty();
        }

        BigDecimal dailyRate = policyService.getPolicy().getDailyFineRate();
        BigDecimal amount = dailyRate.multiply(BigDecimal.valueOf(overdueDays));

        Fine fine = Fine.builder()
                .loan(loan)
                .member(loan.getMember())
                .amount(amount)
                .reason(FineReason.OVERDUE)
                .status(FineStatus.UNPAID)
                .overdueDays((int) overdueDays)
                .dailyRate(dailyRate)
                .finalizedAt(LocalDateTime.now())
                .finalizedBy(librarian)
                .build();

        Fine saved = fineRepository.save(fine);

        // Sinh fineCode sau khi có ID: "FN-YYYY-NNNN"
        String year = String.valueOf(LocalDate.now().getYear());
        saved.setFineCode("FN-" + year + "-" + String.format("%04d", saved.getId()));
        saved = fineRepository.save(saved);

        log.info("Fine {} finalized: Loan={} overdueDays={} amount={} librarian={}",
                saved.getFineCode(), loan.getId(), overdueDays, amount, librarian.getEmail());

        return Optional.of(saved);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Xác nhận thanh toán
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public Fine confirmPaymentAtDesk(Long fineId, User confirmedBy, String transactionRef) {
        Fine fine = getUnpaidFine(fineId);
        fine.setStatus(FineStatus.PAID);
        fine.setPaidAt(LocalDateTime.now());
        fine.setPaymentMethod("AT_DESK");
        fine.setTransactionRef(transactionRef);
        fine.setConfirmedByStaff(confirmedBy);
        fine.setConfirmedByStaffAt(LocalDateTime.now());
        Fine saved = fineRepository.save(fine);
        log.info("Fine {} marked PAID at desk by {}", fine.getFineCode(), confirmedBy.getEmail());
        return saved;
    }

    @Override
    @Transactional
    public Fine confirmPaymentQR(Long fineId, User confirmedBy, String transactionRef) {
        Fine fine = getUnpaidFine(fineId);
        fine.setStatus(FineStatus.PAID);
        fine.setPaidAt(LocalDateTime.now());
        fine.setPaymentMethod("QR_CODE");
        fine.setTransactionRef(transactionRef);
        fine.setConfirmedByStaff(confirmedBy);
        fine.setConfirmedByStaffAt(LocalDateTime.now());
        Fine saved = fineRepository.save(fine);
        log.info("Fine {} marked PAID via QR by {}", fine.getFineCode(), confirmedBy.getEmail());
        return saved;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Member xem danh sách fine
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<Fine> getMemberFines(Long memberId) {
        return fineRepository.findByMember_IdOrderByCreatedAtDesc(memberId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MemberFineDto> getMemberFineDtos(Long memberId) {
        List<MemberFineDto> dtos = new ArrayList<>();
        LocalDate today = LocalDate.now();

        // 1. Khoản phạt ước tính (ESTIMATED) từ các sách đang mượn quá hạn chưa trả
        List<Loan> activeLoans = loanRepository.findByMember_IdAndStatusInOrderByBorrowedAtDesc(
                memberId, List.of(LoanStatus.BORROWING, LoanStatus.OVERDUE, LoanStatus.RETURN_PENDING));

        Set<Long> loansWithExistingFines = fineRepository.findByMember_Id(memberId).stream()
                .filter(f -> f.getLoan() != null)
                .map(f -> f.getLoan().getId())
                .collect(Collectors.toSet());

        for (Loan loan : activeLoans) {
            if (loan.getDueDate() != null && loan.getDueDate().isBefore(today)) {
                if (!loansWithExistingFines.contains(loan.getId())) {
                    long overdueDays = ChronoUnit.DAYS.between(loan.getDueDate(), today);
                    BigDecimal estimated = policyService.getPolicy().getDailyFineRate()
                            .multiply(BigDecimal.valueOf(overdueDays));

                    Book book = loan.getCopy() != null ? loan.getCopy().getBook() : null;
                    String title = book != null ? book.getTitle() : "Book #" + loan.getId();
                    String authors = book != null ? book.getAuthors().stream().map(Author::getName).collect(Collectors.joining(", ")) : "";
                    String cover = book != null ? book.getCoverUrl() : null;
                    String barcode = loan.getCopy() != null ? loan.getCopy().getBarcode() : null;

                    dtos.add(new MemberFineDto(
                            null,
                            loan.getId(),
                            "ESTIMATED",
                            title,
                            authors,
                            cover,
                            barcode,
                            loan.getBorrowedAt() != null ? loan.getBorrowedAt().toLocalDate() : null,
                            loan.getDueDate(),
                            null,
                            overdueDays,
                            estimated,
                            FineStatus.ESTIMATED
                    ));
                }
            }
        }

        // 2. Các khoản phạt đã có trong DB (UNPAID, PAID, WAIVED)
        List<Fine> dbFines = fineRepository.findByMember_IdOrderByCreatedAtDesc(memberId);
        for (Fine f : dbFines) {
            Loan loan = f.getLoan();
            Book book = loan != null && loan.getCopy() != null ? loan.getCopy().getBook() : null;
            String title = book != null ? book.getTitle() : "Book #" + f.getId();
            String authors = book != null ? book.getAuthors().stream().map(Author::getName).collect(Collectors.joining(", ")) : "";
            String cover = book != null ? book.getCoverUrl() : null;
            String barcode = loan != null && loan.getCopy() != null ? loan.getCopy().getBarcode() : null;
            LocalDate borrowedAt = (loan != null && loan.getBorrowedAt() != null) ? loan.getBorrowedAt().toLocalDate() : null;
            LocalDate dueDate = loan != null ? loan.getDueDate() : null;
            LocalDate returnedAt = loan != null && loan.getReturnedAt() != null ? loan.getReturnedAt().toLocalDate() : null;
            long overdueDays = f.getOverdueDays() != null ? f.getOverdueDays() : 0;

            dtos.add(new MemberFineDto(
                    f.getId(),
                    loan != null ? loan.getId() : null,
                    f.getFineCode() != null ? f.getFineCode() : "FN-" + f.getId(),
                    title,
                    authors,
                    cover,
                    barcode,
                    borrowedAt,
                    dueDate,
                    returnedAt,
                    overdueDays,
                    f.getAmount(),
                    f.getStatus()
            ));
        }

        return dtos;
    }

    @Override
    @Transactional(readOnly = true)
    public Fine getFineById(Long fineId) {
        return fineRepository.findById(fineId)
                .orElseThrow(() -> new AccountException(HttpStatus.NOT_FOUND, "Fine record not found: " + fineId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Fine> searchFines(FineStatus status, String keyword) {
        return fineRepository.searchFines(status, (keyword != null && !keyword.isBlank()) ? keyword.trim() : null);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Admin miễn/waive
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public Fine waiveFine(Long fineId, User admin, String reason) {
        Fine fine = fineRepository.findById(fineId)
                .orElseThrow(() -> new AccountException(HttpStatus.NOT_FOUND,
                        "Fine not found: " + fineId));

        if (fine.getStatus() == FineStatus.PAID) {
            throw new AccountException(HttpStatus.BAD_REQUEST,
                    "Cannot waive an already paid fine.");
        }

        fine.setStatus(FineStatus.WAIVED);
        fine.setAdjustedBy(admin);
        fine.setAdjustedAt(LocalDateTime.now());
        fine.setAdjustmentNote(reason);

        Fine saved = fineRepository.save(fine);
        log.info("Fine {} WAIVED by admin {} — reason: {}", fine.getFineCode(), admin.getEmail(), reason);
        return saved;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helper
    // ─────────────────────────────────────────────────────────────────────────

    private Fine getUnpaidFine(Long fineId) {
        Fine fine = fineRepository.findById(fineId)
                .orElseThrow(() -> new AccountException(HttpStatus.NOT_FOUND,
                        "Fine not found: " + fineId));
        if (fine.getStatus() != FineStatus.UNPAID) {
            throw new AccountException(HttpStatus.BAD_REQUEST,
                    "Fine " + fineId + " is not in UNPAID status (current: " + fine.getStatus() + ").");
        }
        return fine;
    }
}
