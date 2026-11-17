package grp3.java.booknet.service.impl.borrowing;

import grp3.java.booknet.dto.loan.*;
import grp3.java.booknet.entity.book.Book;
import grp3.java.booknet.entity.book.BookCopy;
import grp3.java.booknet.entity.book_service.*;
import grp3.java.booknet.entity.user.User;
import grp3.java.booknet.enums.book.CopyStatus;
import grp3.java.booknet.enums.book_service.*;
import grp3.java.booknet.enums.user.UserStatus;
import grp3.java.booknet.exception.borrowing.LoanException;
import grp3.java.booknet.repository.book.BookCopyRepository;
import grp3.java.booknet.repository.book_service.*;
import grp3.java.booknet.repository.user.UserRepository;
import grp3.java.booknet.service.borrowing.LoanService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class LoanServiceImpl implements LoanService {

    private static final int MAX_LOANS_PER_MEMBER = 5;
    private static final int STANDARD_LOAN_DAYS = 14;
    private static final int RENEW_DAYS = 7;
    private static final int MAX_RENEW_COUNT = 1;
    private static final BigDecimal OVERDUE_DAILY_FINE = new BigDecimal("2000.00");
    private static final BigDecimal DAMAGE_FEE = new BigDecimal("50000.00");
    private static final BigDecimal LOST_FEE = new BigDecimal("100000.00");

    private final LoanRepository loanRepository;
    private final LoanItemRepository loanItemRepository;
    private final BorrowRequestRepository borrowRequestRepository;
    private final BorrowRequestItemRepository borrowRequestItemRepository;
    private final ReturnReceiptRepository returnReceiptRepository;
    private final ReturnReceiptItemRepository returnReceiptItemRepository;
    private final BookCopyRepository bookCopyRepository;
    private final FineRepository fineRepository;
    private final ReservationRepository reservationRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public LibrarianDeskSummaryDto getDeskSummary() {
        long pendingApproval = borrowRequestRepository.countByStatus(BorrowRequestStatus.PENDING);
        long awaitingHandover = borrowRequestRepository.countApprovedAwaitingHandover(BorrowRequestStatus.APPROVED);
        long pendingReturns = loanItemRepository.countByStatus(LoanItemStatus.RETURN_REQUESTED);
        List<LoanItemStatus> activeList = List.of(LoanItemStatus.BORROWING, LoanItemStatus.BORROWED, LoanItemStatus.RETURN_REQUESTED);
        long activeBorrowings = loanItemRepository.countByStatusIn(List.of(LoanItemStatus.BORROWING, LoanItemStatus.BORROWED));
        long overdueCount = loanItemRepository.countByStatusInAndDueDateBefore(activeList, LocalDate.now());

        return LibrarianDeskSummaryDto.builder()
                .totalPendingTasks(pendingApproval + awaitingHandover + pendingReturns)
                .pendingApprovalCount(pendingApproval)
                .awaitingHandoverCount(awaitingHandover)
                .pendingReturnsCount(pendingReturns)
                .activeBorrowingsCount(activeBorrowings)
                .overdueCount(overdueCount)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BorrowRequestSummaryDto> getPendingBorrowRequests(Pageable pageable) {
        return borrowRequestRepository.findByStatus(BorrowRequestStatus.PENDING, pageable)
                .map(this::mapToRequestSummary);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BorrowRequestSummaryDto> getApprovedAwaitingHandover(Pageable pageable) {
        return borrowRequestRepository.findApprovedAwaitingHandover(BorrowRequestStatus.APPROVED, pageable)
                .map(this::mapToRequestSummary);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<LoanItemDetailDto> getPendingReturns(Pageable pageable) {
        return loanItemRepository.findByStatus(LoanItemStatus.RETURN_REQUESTED, pageable)
                .map(this::mapToLoanItemDetail);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<LoanItemDetailDto> getActiveBorrowings(Pageable pageable) {
        return loanItemRepository.findByStatusIn(List.of(LoanItemStatus.BORROWING, LoanItemStatus.BORROWED), pageable)
                .map(this::mapToLoanItemDetail);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<LoanItemDetailDto> getLoanHistory(Pageable pageable) {
        List<LoanItemStatus> historyStatuses = List.of(LoanItemStatus.RETURNED, LoanItemStatus.LOST, LoanItemStatus.DAMAGED);
        return loanItemRepository.findByStatusIn(historyStatuses, pageable)
                .map(this::mapToLoanItemDetail);
    }

    private Optional<User> findUserByEmail(String email) {
        if (email == null || email.isBlank()) {
            return Optional.empty();
        }
        String cleanEmail = email.trim();
        Optional<User> direct = userRepository.findByEmail(cleanEmail);
        if (direct.isPresent()) {
            return direct;
        }
        direct = userRepository.findByEmail(cleanEmail.toLowerCase());
        if (direct.isPresent()) {
            return direct;
        }
        return userRepository.findAll().stream()
                .filter(u -> u.getEmail() != null && u.getEmail().equalsIgnoreCase(cleanEmail))
                .findFirst();
    }

    @Override
    @Transactional(readOnly = true)
    public MemberCirculationInfoDto lookupMember(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return null;
        }
        String term = keyword.trim().toLowerCase();
        Optional<User> userOpt = findUserByEmail(term);
        if (userOpt.isEmpty()) {
            userOpt = userRepository.findByMemberCode(term.toUpperCase());
        }
        if (userOpt.isEmpty()) {
            userOpt = userRepository.findByMemberCode(keyword.trim());
        }
        if (userOpt.isEmpty()) {
            userOpt = userRepository.findAll().stream()
                    .filter(u -> u.getFullName() != null && u.getFullName().toLowerCase().contains(term))
                    .findFirst();
        }

        if (userOpt.isEmpty()) {
            return null;
        }

        User member = userOpt.get();
        List<LoanItemStatus> activeStatuses = List.of(LoanItemStatus.BORROWING, LoanItemStatus.BORROWED, LoanItemStatus.RETURN_REQUESTED);
        List<LoanItem> activeItems = loanItemRepository.findByLoan_Member_IdAndStatusIn(member.getId(), activeStatuses);

        int activeLoansCount = activeItems.size();
        List<Fine> unpaidFinesList = fineRepository.findByMember_IdAndStatus(member.getId(), FineStatus.UNPAID);
        BigDecimal unpaidFines = unpaidFinesList.stream()
                .map(Fine::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        LocalDate today = LocalDate.now();
        boolean hasOverdue = loanItemRepository.existsByLoan_Member_IdAndStatusInAndDueDateBefore(member.getId(), activeStatuses, today);

        boolean cardActive = member.getStatus() == UserStatus.ACTIVE;
        boolean withinLimit = activeLoansCount < MAX_LOANS_PER_MEMBER;
        boolean noFines = unpaidFines.compareTo(BigDecimal.ZERO) <= 0;
        boolean eligible = cardActive && withinLimit && noFines && !hasOverdue;

        StringBuilder reason = new StringBuilder();
        if (!cardActive) reason.append("Member card is not active (").append(member.getStatus()).append("). ");
        if (!withinLimit) reason.append("Max loan limit reached (").append(activeLoansCount).append("/").append(MAX_LOANS_PER_MEMBER).append("). ");
        if (!noFines) reason.append("Unpaid fine balance of ").append(unpaidFines).append(" VND. ");
        if (hasOverdue) reason.append("Member has overdue book(s) in possession. ");

        List<LoanItemDetailDto> currentBorrowings = activeItems.stream()
                .map(this::mapToLoanItemDetail)
                .toList();

        return MemberCirculationInfoDto.builder()
                .memberId(member.getId())
                .fullName(member.getFullName())
                .email(member.getEmail())
                .phone(member.getPhone())
                .memberCode(member.getMemberCode())
                .cardStatus(member.getStatus().name())
                .activeLoansCount(activeLoansCount)
                .maxAllowedLoans(MAX_LOANS_PER_MEMBER)
                .unpaidFines(unpaidFines)
                .hasOverdue(hasOverdue)
                .eligible(eligible)
                .ineligibilityReason(reason.toString().trim())
                .currentBorrowings(currentBorrowings)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public BorrowRequestSummaryDto getBorrowRequestDetails(Long requestId) {
        BorrowRequest req = borrowRequestRepository.findById(requestId)
                .orElseThrow(() -> new LoanException("Borrow request not found: " + requestId));
        return mapToRequestSummary(req);
    }

    @Override
    public void approveBorrowRequest(Long requestId, String librarianEmail) {
        BorrowRequest req = borrowRequestRepository.findById(requestId)
                .orElseThrow(() -> new LoanException("Borrow request not found: " + requestId));

        if (req.getStatus() != BorrowRequestStatus.PENDING) {
            throw new LoanException("Request is not in PENDING status: " + req.getStatus());
        }

        MemberCirculationInfoDto memberInfo = lookupMember(req.getMember().getEmail());
        if (memberInfo != null && !memberInfo.isEligible()) {
            throw new LoanException("Member is ineligible: " + memberInfo.getIneligibilityReason());
        }

        User librarian = findUserByEmail(librarianEmail).orElse(null);
        req.setStatus(BorrowRequestStatus.APPROVED);
        req.setApprovedAt(LocalDateTime.now());
        req.setApprovedBy(librarian);
        borrowRequestRepository.save(req);
    }

    @Override
    public void rejectBorrowRequest(Long requestId, String reason, String librarianEmail) {
        BorrowRequest req = borrowRequestRepository.findById(requestId)
                .orElseThrow(() -> new LoanException("Borrow request not found: " + requestId));

        if (req.getStatus() != BorrowRequestStatus.PENDING) {
            throw new LoanException("Request is not in PENDING status: " + req.getStatus());
        }

        req.setStatus(BorrowRequestStatus.REJECTED);
        req.setRejectionReason(reason != null && !reason.isBlank() ? reason : "Rejected by librarian");
        borrowRequestRepository.save(req);
    }

    @Override
    public Loan handoverLoan(Long requestId, List<String> copyBarcodes, String notes, String librarianEmail) {
        BorrowRequest req = borrowRequestRepository.findById(requestId)
                .orElseThrow(() -> new LoanException("Borrow request not found: " + requestId));

        if (req.getStatus() != BorrowRequestStatus.APPROVED && req.getStatus() != BorrowRequestStatus.PENDING) {
            throw new LoanException("Request cannot be checked out with status: " + req.getStatus());
        }

        if (req.getStatus() == BorrowRequestStatus.COMPLETED) {
            throw new LoanException("This request has already been handed over into an active loan.");
        }

        User librarian = findUserByEmail(librarianEmail).orElse(null);
        LocalDateTime checkoutTime = LocalDateTime.now();
        LocalDate dueDate = checkoutTime.toLocalDate().plusDays(STANDARD_LOAN_DAYS);

        BookCopy primaryCopy = bookCopyRepository.findByBarcode(copyBarcodes.get(0))
                .orElseThrow(() -> new LoanException("Physical copy not found for barcode: " + copyBarcodes.get(0)));

        Loan loan = Loan.builder()
                .copy(primaryCopy)
                .member(req.getMember())
                .issuedBy(librarian)
                .borrowedAt(checkoutTime)
                .dueDate(dueDate)
                .status(LoanStatus.BORROWING)
                .renewCount(0)
                .note(notes != null && notes.length() > 255 ? notes.substring(0, 255) : notes)
                .build();

        loan = loanRepository.save(loan);

        List<BorrowRequestItem> reqItems = borrowRequestItemRepository.findByRequest_Id(req.getId());
        for (int i = 0; i < reqItems.size(); i++) {
            BorrowRequestItem rItem = reqItems.get(i);
            String barcode = (copyBarcodes != null && i < copyBarcodes.size()) ? copyBarcodes.get(i).trim() : null;

            BookCopy copy = null;
            if (barcode != null && !barcode.isEmpty()) {
                copy = bookCopyRepository.findByBarcode(barcode)
                        .orElseGet(() -> bookCopyRepository.findAll().stream()
                                .filter(c -> barcode.equalsIgnoreCase(c.getBarcode()))
                                .findFirst()
                                .orElseThrow(() -> new LoanException("Physical copy not found with barcode: " + barcode)));

                if (!Objects.equals(copy.getBook().getId(), rItem.getBook().getId())) {
                    throw new LoanException("Copy " + barcode + " does not belong to the requested book: " + rItem.getBook().getTitle());
                }
                if (copy.getStatus() != CopyStatus.AVAILABLE) {
                    throw new LoanException("Copy " + barcode + " is currently " + copy.getStatus() + " and not available for checkout.");
                }
            } else {
                copy = bookCopyRepository.findFirstByBook_IdAndStatus(rItem.getBook().getId(), CopyStatus.AVAILABLE)
                        .orElseThrow(() -> new LoanException("No available copies for book: " + rItem.getBook().getTitle()));
            }

            copy.setStatus(CopyStatus.BORROWED);
            bookCopyRepository.save(copy);

            rItem.setAssignedCopy(copy);
            borrowRequestItemRepository.save(rItem);

            if (loan.getCopy() == null) {
                loan.setCopy(copy);
            }

            LoanItem lItem = LoanItem.builder()
                    .loan(loan)
                    .copy(copy)
                    .status(LoanItemStatus.BORROWING)
                    .dueDate(dueDate)
                    .renewCount(0)
                    .build();
            loanItemRepository.save(lItem);
        }

        loan = loanRepository.save(loan);

        req.setStatus(BorrowRequestStatus.COMPLETED);
        if (req.getApprovedAt() == null) {
            req.setApprovedAt(checkoutTime);
        }
        if (req.getApprovedBy() == null && librarian != null) {
            req.setApprovedBy(librarian);
        }
        borrowRequestRepository.save(req);

        return loan;
    }

    @Override
    public ReturnReceiptViewDto receiveReturn(String barcode, ReturnCondition condition, String conditionNotes, String librarianEmail) {
        if (barcode == null || barcode.trim().isEmpty()) {
            throw new LoanException("Barcode is required to receive return.");
        }
        String cleanBarcode = barcode.trim();

        List<LoanItemStatus> activeStatuses = List.of(LoanItemStatus.BORROWING, LoanItemStatus.BORROWED, LoanItemStatus.RETURN_REQUESTED);
        LoanItem loanItem = loanItemRepository.findByCopy_BarcodeIgnoreCaseAndStatusIn(cleanBarcode, activeStatuses)
                .or(() -> loanItemRepository.findByCopy_BarcodeAndStatusIn(cleanBarcode, activeStatuses))
                .orElseThrow(() -> new LoanException("No active circulation item found with barcode: " + cleanBarcode));

        User librarian = findUserByEmail(librarianEmail).orElse(null);
        if (librarian == null && loanItem.getLoan() != null) {
            librarian = loanItem.getLoan().getIssuedBy();
        }
        if (librarian == null && loanItem.getLoan() != null) {
            librarian = loanItem.getLoan().getMember();
        }
        LocalDateTime receiveTime = LocalDateTime.now();
        LocalDate today = receiveTime.toLocalDate();

        BigDecimal overdueFine = BigDecimal.ZERO;
        if (today.isAfter(loanItem.getDueDate())) {
            long daysOverdue = ChronoUnit.DAYS.between(loanItem.getDueDate(), today);
            overdueFine = OVERDUE_DAILY_FINE.multiply(BigDecimal.valueOf(daysOverdue));
        }

        BigDecimal damageFine = BigDecimal.ZERO;
        ReturnCondition finalCondition = condition != null ? condition : ReturnCondition.GOOD;
        if (finalCondition == ReturnCondition.DAMAGED) {
            damageFine = DAMAGE_FEE;
        } else if (finalCondition == ReturnCondition.LOST) {
            damageFine = LOST_FEE;
        }

        BigDecimal totalFine = overdueFine.add(damageFine);

        LoanItemStatus finalStatus = LoanItemStatus.RETURNED;
        if (finalCondition == ReturnCondition.DAMAGED) finalStatus = LoanItemStatus.DAMAGED;
        if (finalCondition == ReturnCondition.LOST) finalStatus = LoanItemStatus.LOST;

        loanItem.setStatus(finalStatus);
        loanItem.setReturnedAt(receiveTime);
        loanItem.setReturnCondition(finalCondition);
        loanItem.setDamageNotes(conditionNotes);
        loanItem.setFineAmount(totalFine);
        loanItemRepository.save(loanItem);

        Loan loan = loanItem.getLoan();
        List<LoanItem> allLoanItems = loanItemRepository.findByLoan_Id(loan.getId());
        boolean allFinished = !allLoanItems.isEmpty() && allLoanItems.stream().allMatch(i ->
                i.getStatus() == LoanItemStatus.RETURNED || i.getStatus() == LoanItemStatus.DAMAGED || i.getStatus() == LoanItemStatus.LOST);
        if (allFinished) {
            loan.setStatus(LoanStatus.RETURNED);
            loan.setReturnedAt(receiveTime);
            if (librarian != null) {
                loan.setReceivedBy(librarian);
            }
            loanRepository.save(loan);
        }

        BookCopy copy = loanItem.getCopy();
        if (finalCondition == ReturnCondition.LOST) {
            copy.setStatus(CopyStatus.LOST);
        } else if (finalCondition == ReturnCondition.DAMAGED) {
            copy.setStatus(CopyStatus.DAMAGED);
        } else {
            Long bookId = copy.getBook().getId();
            Optional<Reservation> resvOpt = reservationRepository.findAll().stream()
                    .filter(r -> r.getBook().getId().equals(bookId) && r.getStatus() == ReservationStatus.PENDING)
                    .min(Comparator.comparing(Reservation::getReservedAt));
            if (resvOpt.isPresent()) {
                Reservation resv = resvOpt.get();
                resv.setStatus(ReservationStatus.READY);
                resv.setAssignedCopy(copy);
                resv.setReadyAt(receiveTime);
                resv.setPickupUntil(receiveTime.plusDays(2));
                reservationRepository.save(resv);
                copy.setStatus(CopyStatus.RESERVED);
            } else {
                copy.setStatus(CopyStatus.AVAILABLE);
            }
        }
        bookCopyRepository.save(copy);

        if (overdueFine.compareTo(BigDecimal.ZERO) > 0) {
            Fine fine = Fine.builder()
                    .loan(loan)
                    .member(loan.getMember())
                    .amount(overdueFine)
                    .reason(FineReason.OVERDUE)
                    .status(FineStatus.UNPAID)
                    .build();
            fineRepository.save(fine);
        }
        if (damageFine.compareTo(BigDecimal.ZERO) > 0) {
            Fine fine = Fine.builder()
                    .loan(loan)
                    .member(loan.getMember())
                    .amount(damageFine)
                    .reason(finalCondition == ReturnCondition.LOST ? FineReason.LOST : FineReason.DAMAGED)
                    .status(FineStatus.UNPAID)
                    .build();
            fineRepository.save(fine);
        }

        String receiptCode = "RR-" + receiveTime.format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "-" + String.format("%04d", (int)(Math.random() * 9000) + 1000);
        ReturnReceipt receipt = ReturnReceipt.builder()
                .receiptCode(receiptCode)
                .loan(loan)
                .member(loan.getMember())
                .receivedBy(librarian)
                .returnedAt(receiveTime)
                .returnDate(receiveTime)
                .totalFines(totalFine)
                .totalFineAmount(totalFine)
                .notes(conditionNotes)
                .note(conditionNotes)
                .build();
        receipt = returnReceiptRepository.save(receipt);

        int overdueDaysCount = (int) Math.max(0, ChronoUnit.DAYS.between(loanItem.getDueDate(), today));

        ReturnReceiptItem rItem = ReturnReceiptItem.builder()
                .receipt(receipt)
                .loanItem(loanItem)
                .copy(copy)
                .itemCondition(finalCondition)
                .condition(finalCondition)
                .overdueDays(overdueDaysCount)
                .conditionNotes(conditionNotes)
                .note(conditionNotes)
                .fineAmount(totalFine)
                .build();
        returnReceiptItemRepository.save(rItem);
        receipt.addItem(rItem);

        return mapToReceiptView(receipt);
    }

    @Override
    @Transactional(readOnly = true)
    public ReturnReceiptViewDto getReturnReceipt(String receiptCode) {
        ReturnReceipt receipt = returnReceiptRepository.findByReceiptCode(receiptCode)
                .orElseThrow(() -> new LoanException("Receipt not found: " + receiptCode));
        return mapToReceiptView(receipt);
    }

    @Override
    public LoanItem renewLoanItem(Long loanItemId, String librarianEmail) {
        LoanItem item = loanItemRepository.findById(loanItemId)
                .orElseThrow(() -> new LoanException("Loan item not found: " + loanItemId));

        if (item.getStatus() != LoanItemStatus.BORROWING && item.getStatus() != LoanItemStatus.BORROWED) {
            throw new LoanException("Only active borrowings can be renewed. Current status: " + item.getStatus());
        }

        if (item.getRenewCount() >= MAX_RENEW_COUNT) {
            throw new LoanException("Renewal limit reached. This book has already been renewed once.");
        }

        LocalDate today = LocalDate.now();
        if (today.isAfter(item.getDueDate())) {
            throw new LoanException("Cannot renew overdue book. Overdue items must be returned and cleared.");
        }

        Long bookId = item.getCopy().getBook().getId();
        boolean hasWaitingReservation = reservationRepository.findAll().stream()
                .anyMatch(r -> r.getBook().getId().equals(bookId) && r.getStatus() == ReservationStatus.PENDING);
        if (hasWaitingReservation) {
            throw new LoanException("Cannot renew book: another reader is currently waiting in reservation queue.");
        }

        item.setDueDate(item.getDueDate().plusDays(RENEW_DAYS));
        item.setRenewCount(item.getRenewCount() + 1);
        item.setLastRenewedAt(LocalDateTime.now());
        loanItemRepository.save(item);

        Loan parentLoan = item.getLoan();
        if (parentLoan != null) {
            parentLoan.setRenewCount(parentLoan.getRenewCount() + 1);
            if (parentLoan.getStatus() == LoanStatus.RENEW_PENDING) {
                parentLoan.setStatus(LoanStatus.BORROWING);
            }
            if (item.getDueDate().isAfter(parentLoan.getDueDate())) {
                parentLoan.setDueDate(item.getDueDate());
            }
            loanRepository.save(parentLoan);
        }

        return item;
    }

    private BorrowRequestSummaryDto mapToRequestSummary(BorrowRequest req) {
        List<BorrowRequestItem> items = borrowRequestItemRepository.findByRequest_Id(req.getId());
        List<String> titles = items.stream().map(i -> i.getBook().getTitle()).toList();
        List<Long> bookIds = items.stream().map(i -> i.getBook().getId()).toList();

        return BorrowRequestSummaryDto.builder()
                .requestId(req.getId())
                .requestCode(req.getRequestCode())
                .memberId(req.getMember().getId())
                .memberName(req.getMember().getFullName())
                .memberEmail(req.getMember().getEmail())
                .memberPhone(req.getMember().getPhone())
                .memberCode(req.getMember().getMemberCode())
                .requestDate(req.getRequestDate())
                .status(req.getStatus())
                .bookTitles(titles)
                .bookIds(bookIds)
                .bookCount(items.size())
                .rejectionReason(req.getRejectionReason())
                .build();
    }

    private LoanItemDetailDto mapToLoanItemDetail(LoanItem item) {
        LocalDate today = LocalDate.now();
        boolean isOverdue = today.isAfter(item.getDueDate()) && (item.getStatus() == LoanItemStatus.BORROWING || item.getStatus() == LoanItemStatus.BORROWED);
        long daysOverdue = isOverdue ? ChronoUnit.DAYS.between(item.getDueDate(), today) : 0;

        return LoanItemDetailDto.builder()
                .loanItemId(item.getId())
                .loanId(item.getLoan().getId())
                .loanCode("LN-2026-" + String.format("%04d", item.getLoan().getId()))
                .memberName(item.getLoan().getMember().getFullName())
                .memberEmail(item.getLoan().getMember().getEmail())
                .copyBarcode(item.getCopy().getBarcode())
                .bookTitle(item.getCopy().getBook().getTitle())
                .dueDate(item.getDueDate())
                .borrowedAt(item.getLoan().getBorrowedAt())
                .returnedAt(item.getReturnedAt())
                .renewCount(item.getRenewCount())
                .status(item.getStatus().name())
                .overdue(isOverdue)
                .daysOverdue(daysOverdue)
                .fineAmount(item.getFineAmount())
                .returnCondition(item.getReturnCondition() != null ? item.getReturnCondition().name() : null)
                .damageNotes(item.getDamageNotes())
                .build();
    }

    private ReturnReceiptViewDto mapToReceiptView(ReturnReceipt receipt) {
        List<ReturnReceiptItem> items = returnReceiptItemRepository.findByReceipt_Id(receipt.getId());
        List<ReturnReceiptItemViewDto> itemDtos = items.stream().map(i -> ReturnReceiptItemViewDto.builder()
                .bookTitle(i.getCopy().getBook().getTitle())
                .copyBarcode(i.getCopy().getBarcode())
                .condition(i.getCondition().name())
                .conditionNotes(i.getConditionNotes())
                .fineAmount(i.getFineAmount())
                .build()
        ).toList();

        return ReturnReceiptViewDto.builder()
                .receiptCode(receipt.getReceiptCode())
                .loanCode(receipt.getLoan() != null ? "LN-2026-" + String.format("%04d", receipt.getLoan().getId()) : null)
                .memberName(receipt.getMember().getFullName())
                .memberEmail(receipt.getMember().getEmail())
                .memberCode(receipt.getMember().getMemberCode())
                .librarianName(receipt.getReceivedBy() != null ? receipt.getReceivedBy().getFullName() : "Librarian Desk Staff")
                .returnDate(receipt.getReturnDate())
                .totalFines(receipt.getTotalFines())
                .notes(receipt.getNotes())
                .items(itemDtos)
                .build();
    }
}
