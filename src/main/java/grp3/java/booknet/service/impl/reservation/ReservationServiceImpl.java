package grp3.java.booknet.service.impl.reservation;

import grp3.java.booknet.dto.reservation.LibrarianReservationsView;
import grp3.java.booknet.dto.reservation.MemberReservationsView;
import grp3.java.booknet.entity.book.Author;
import grp3.java.booknet.entity.book.Book;
import grp3.java.booknet.entity.book.BookCopy;
import grp3.java.booknet.entity.book.Category;
import grp3.java.booknet.entity.book_service.Loan;
import grp3.java.booknet.entity.book_service.LoanItem;
import grp3.java.booknet.entity.book_service.Reservation;
import grp3.java.booknet.entity.user.User;
import grp3.java.booknet.enums.book.CopyStatus;
import grp3.java.booknet.enums.book.PublicationStatus;
import grp3.java.booknet.enums.book_service.FineStatus;
import grp3.java.booknet.enums.book_service.LoanItemStatus;
import grp3.java.booknet.enums.book_service.LoanStatus;
import grp3.java.booknet.enums.book_service.ReservationStatus;
import grp3.java.booknet.exception.reservation.ReservationException;
import grp3.java.booknet.repository.book.BookCopyRepository;
import grp3.java.booknet.repository.book.BookRepository;
import grp3.java.booknet.repository.book_service.FineRepository;
import grp3.java.booknet.repository.book_service.LoanItemRepository;
import grp3.java.booknet.repository.book_service.LoanRepository;
import grp3.java.booknet.repository.book_service.ReservationRepository;
import grp3.java.booknet.repository.user.UserRepository;
import grp3.java.booknet.service.book.BookInventoryService;
import grp3.java.booknet.service.member.MemberEligibilityService;
import grp3.java.booknet.service.notification.NotificationService;
import grp3.java.booknet.service.reservation.ReservationQueueService;
import grp3.java.booknet.service.reservation.ReservationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReservationServiceImpl implements ReservationService, ReservationQueueService {

    public static final int MAX_ACTIVE_RESERVATIONS = 5;
    public static final int HOLD_DURATION_DAYS = 2;
    public static final int MAX_LOAN_LIMIT = 5;
    public static final int STANDARD_LOAN_DAYS = 14;

    private static final String DEFAULT_COVER = "/images/home/cover-hoa-vang.jpg";
    private static final String DEFAULT_PICKUP_LOCATION = "Circulation Desk · Floor 1";

    private static final Set<ReservationStatus> ACTIVE_RESERVATION_STATUSES = EnumSet.of(
            ReservationStatus.PENDING,
            ReservationStatus.READY
    );

    private static final Set<ReservationStatus> CLOSED_RESERVATION_STATUSES = EnumSet.of(
            ReservationStatus.FULFILLED,
            ReservationStatus.CANCELLED,
            ReservationStatus.EXPIRED
    );

    private static final Set<LoanStatus> ACTIVE_LOAN_STATUSES = EnumSet.of(
            LoanStatus.BORROWING,
            LoanStatus.RENEW_PENDING,
            LoanStatus.RETURN_PENDING,
            LoanStatus.OVERDUE
    );

    private final ReservationRepository reservationRepository;
    private final BookRepository bookRepository;
    private final BookCopyRepository bookCopyRepository;
    private final LoanRepository loanRepository;
    private final LoanItemRepository loanItemRepository;
    private final FineRepository fineRepository;
    private final UserRepository userRepository;
    private final BookInventoryService bookInventoryService;
    private final MemberEligibilityService memberEligibilityService;
    private final NotificationService notificationService;

    @Override
    @Transactional(readOnly = true)
    public boolean hasWaitingQueueForRenewal(Long bookId, Long borrowerMemberId) {
        if (bookId == null) {
            return false;
        }
        if (borrowerMemberId != null) {
            return reservationRepository.existsByBook_IdAndStatusInAndMember_IdNot(
                    bookId, List.of(ReservationStatus.PENDING), borrowerMemberId);
        }
        return reservationRepository.existsByBook_IdAndStatusIn(bookId, List.of(ReservationStatus.PENDING));
    }

    @Override
    @Transactional
    public Optional<Reservation> assignCopyToNextPendingReservation(BookCopy copy, LocalDateTime now) {
        if (copy == null || copy.getId() == null) {
            return Optional.empty();
        }
        LocalDateTime actionTime = now != null ? now : LocalDateTime.now();
        BookCopy lockedCopy = bookCopyRepository.findByIdForUpdate(copy.getId()).orElse(copy);
        Book book = lockedCopy.getBook();
        if (book == null) {
            return Optional.empty();
        }

        boolean bookAllowsCirculation = !book.isDeleted()
                && book.getPublicationStatus() != PublicationStatus.DISCONTINUED;

        CopyStatus status = lockedCopy.getStatus();
        if (!bookAllowsCirculation) {
            releaseCopyBackToShelf(lockedCopy.getId(), status);
            return Optional.empty();
        }

        if (status != CopyStatus.BORROWED && status != CopyStatus.AVAILABLE && status != CopyStatus.RESERVED) {
            return Optional.empty();
        }

        Optional<Reservation> nextPendingOpt = reservationRepository
                .findFirstByBook_IdAndStatusOrderByReservedAtAscIdAsc(book.getId(), ReservationStatus.PENDING);

        if (nextPendingOpt.isEmpty()) {
            releaseCopyBackToShelf(lockedCopy.getId(), status);
            return Optional.empty();
        }

        // BookInventoryService requires BORROWED/RESERVED -> AVAILABLE before AVAILABLE -> RESERVED.
        releaseCopyBackToShelf(lockedCopy.getId(), status);
        BookCopy heldCopy = bookInventoryService.markReserved(lockedCopy.getId());

        Reservation next = nextPendingOpt.get();
        next.setStatus(ReservationStatus.READY);
        next.setAssignedCopy(heldCopy);
        next.setReadyAt(actionTime);
        next.setPickupUntil(actionTime.plusDays(HOLD_DURATION_DAYS));
        Reservation saved = reservationRepository.saveAndFlush(next);
        notificationService.notifyReservationReady(saved);

        log.info("Assigned copy {} to reservation {} for member {}",
                heldCopy.getBarcode(), saved.displayCode(), saved.getMember().getId());
        return Optional.of(saved);
    }

    @Override
    @Transactional
    public int expireOverdueReservations() {
        return expireOverdueReservations(LocalDateTime.now());
    }

    @Override
    @Transactional
    public int expireOverdueReservations(LocalDateTime asOf) {
        LocalDateTime cutoff = asOf != null ? asOf : LocalDateTime.now();
        List<Reservation> candidates = reservationRepository.findByStatusAndPickupUntilBefore(
                ReservationStatus.READY, cutoff.plusNanos(1));
        if (candidates.isEmpty()) {
            return 0;
        }

        int expiredCount = 0;
        for (Reservation candidate : candidates) {
            Reservation locked = reservationRepository.findLockedById(candidate.getId()).orElse(null);
            if (locked == null || locked.getStatus() != ReservationStatus.READY) {
                continue;
            }
            if (locked.getPickupUntil() == null || locked.getPickupUntil().isAfter(cutoff)) {
                continue;
            }

            BookCopy heldCopy = locked.getAssignedCopy();
            String heldBarcode = heldCopy != null ? heldCopy.getBarcode() : null;
            locked.setStatus(ReservationStatus.EXPIRED);
            locked.setExpiredAt(cutoff);
            locked.setAssignedCopy(null);
            if (locked.getNotes() == null || locked.getNotes().isBlank()) {
                locked.setNotes(heldBarcode != null
                        ? "Pickup window expired; released copy " + heldBarcode
                        : "Pickup window expired");
            }
            Reservation saved = reservationRepository.saveAndFlush(locked);
            notificationService.notifyReservationExpired(saved);
            expiredCount++;

            if (heldCopy != null) {
                assignCopyToNextPendingReservation(heldCopy, cutoff);
            }
        }
        return expiredCount;
    }

    @Override
    @Transactional
    public MemberReservationsView getMemberReservationsView(Long memberId, Long cancelId) {
        findUserOrThrow(memberId);
        expireOverdueReservations();

        List<Reservation> all = reservationRepository.findByMember_IdOrderByReservedAtDescIdDesc(memberId);
        List<Reservation> activeRaw = new ArrayList<>();
        List<Reservation> completedRaw = new ArrayList<>();
        List<Reservation> canceledRaw = new ArrayList<>();

        for (Reservation r : all) {
            if (r.getStatus() == ReservationStatus.PENDING || r.getStatus() == ReservationStatus.READY) {
                activeRaw.add(r);
            } else if (r.getStatus() == ReservationStatus.FULFILLED) {
                completedRaw.add(r);
            } else {
                canceledRaw.add(r);
            }
        }

        activeRaw.sort(Comparator
                .comparing((Reservation r) -> r.getStatus() == ReservationStatus.READY ? 0 : 1)
                .thenComparing(Reservation::getPickupUntil, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(Reservation::getReservedAt, Comparator.nullsLast(Comparator.naturalOrder())));

        List<MemberReservationsView.ReservationCardDto> activeCards = activeRaw.stream()
                .map(this::toMemberCardDto)
                .toList();
        List<MemberReservationsView.ReservationCardDto> completedCards = completedRaw.stream()
                .map(this::toMemberCardDto)
                .toList();
        List<MemberReservationsView.ReservationCardDto> canceledCards = canceledRaw.stream()
                .map(this::toMemberCardDto)
                .toList();

        MemberReservationsView.ReadyPickupBanner readyBanner = activeRaw.stream()
                .filter(r -> r.getStatus() == ReservationStatus.READY)
                .findFirst()
                .map(r -> new MemberReservationsView.ReadyPickupBanner(
                        r.getBook().getTitle(),
                        r.getPickupUntil(),
                        r.getMember().getMemberCode()))
                .orElse(null);

        MemberReservationsView.ReservationCardDto cancelCandidate = null;
        if (cancelId != null) {
            cancelCandidate = activeCards.stream()
                    .filter(c -> c.reservationId().equals(cancelId))
                    .findFirst()
                    .orElse(null);
        }

        return new MemberReservationsView(
                activeCards,
                completedCards,
                canceledCards,
                readyBanner,
                activeCards.size(),
                completedCards.size(),
                canceledCards.size(),
                MAX_ACTIVE_RESERVATIONS,
                cancelCandidate
        );
    }

    @Override
    @Transactional
    public Reservation reserveBook(Long memberId, Long bookId) {
        User member = findUserOrThrow(memberId);
        boolean hasReserveCap = member.effectiveAuthorities().stream()
                .anyMatch(a -> "RESERVE".equals(a.getAuthority()));
        if (!hasReserveCap) {
            throw new ReservationException(HttpStatus.FORBIDDEN,
                    "Your account does not have permission to reserve books.");
        }
        if (!memberEligibilityService.isEligible(memberId)) {
            throw new ReservationException(HttpStatus.BAD_REQUEST,
                    "Your account or library card must be active to place a reservation.");
        }
        if (fineRepository.existsByMember_IdAndStatus(memberId, FineStatus.UNPAID)) {
            throw new ReservationException(HttpStatus.BAD_REQUEST,
                    "Please clear unpaid library fines before placing new reservations.");
        }

        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new ReservationException(HttpStatus.NOT_FOUND, "Book not found."));
        if (book.isDeleted() || book.getPublicationStatus() != PublicationStatus.PUBLISHED) {
            throw new ReservationException(HttpStatus.BAD_REQUEST,
                    "This title is not currently open for member reservations.");
        }

        long available = bookCopyRepository.countByBook_IdAndStatus(bookId, CopyStatus.AVAILABLE);
        if (available > 0) {
            throw new ReservationException(HttpStatus.BAD_REQUEST,
                    "Copies are currently available on the shelf. Please request a regular loan instead.");
        }

        long circulating = bookCopyRepository.countByBook_IdAndStatus(bookId, CopyStatus.BORROWED)
                + bookCopyRepository.countByBook_IdAndStatus(bookId, CopyStatus.RESERVED);
        if (circulating <= 0) {
            throw new ReservationException(HttpStatus.BAD_REQUEST,
                    "This book has no circulating copies available for queuing.");
        }

        if (loanRepository.existsByMember_IdAndCopy_Book_IdAndStatusIn(memberId, bookId, ACTIVE_LOAN_STATUSES)) {
            throw new ReservationException(HttpStatus.BAD_REQUEST,
                    "You are currently borrowing a copy of this book.");
        }

        if (reservationRepository.existsByMember_IdAndBook_IdAndStatusIn(memberId, bookId, ACTIVE_RESERVATION_STATUSES)) {
            throw new ReservationException(HttpStatus.BAD_REQUEST,
                    "You already have an active reservation for this book.");
        }

        long activeCount = reservationRepository.countByMember_IdAndStatusIn(memberId, ACTIVE_RESERVATION_STATUSES);
        if (activeCount >= MAX_ACTIVE_RESERVATIONS) {
            throw new ReservationException(HttpStatus.BAD_REQUEST,
                    "You have reached the maximum of " + MAX_ACTIVE_RESERVATIONS + " active reservations.");
        }

        Reservation saved = reservationRepository.saveAndFlush(Reservation.builder()
                .book(book)
                .member(member)
                .status(ReservationStatus.PENDING)
                .reservedAt(LocalDateTime.now())
                .build());

        notificationService.notifyReservationCreated(saved);
        log.info("Member {} placed reservation {} for book {}", memberId, saved.displayCode(), bookId);
        return saved;
    }

    @Override
    @Transactional
    public void cancelMemberReservation(Long memberId, Long reservationId) {
        findUserOrThrow(memberId);
        Reservation locked = reservationRepository.findLockedById(reservationId)
                .orElseThrow(() -> new ReservationException(HttpStatus.NOT_FOUND, "Reservation not found."));

        if (!locked.getMember().getId().equals(memberId)) {
            throw new ReservationException(HttpStatus.FORBIDDEN, "You do not have permission to cancel this reservation.");
        }
        if (!ACTIVE_RESERVATION_STATUSES.contains(locked.getStatus())) {
            throw new ReservationException(HttpStatus.BAD_REQUEST, "Only active reservations can be canceled.");
        }

        LocalDateTime now = LocalDateTime.now();
        BookCopy heldCopy = locked.getStatus() == ReservationStatus.READY ? locked.getAssignedCopy() : null;
        String heldBarcode = heldCopy != null ? heldCopy.getBarcode() : null;

        locked.setStatus(ReservationStatus.CANCELLED);
        locked.setCancelledAt(now);
        locked.setAssignedCopy(null);
        if (locked.getNotes() == null || locked.getNotes().isBlank()) {
            locked.setNotes(heldBarcode != null
                    ? "Canceled by member; released copy " + heldBarcode
                    : "Canceled by member");
        }
        Reservation saved = reservationRepository.saveAndFlush(locked);
        notificationService.notifyReservationCancelled(saved, false);

        if (heldCopy != null) {
            assignCopyToNextPendingReservation(heldCopy, now);
        }
    }

    @Override
    @Transactional
    public LibrarianReservationsView getLibrarianReservationsView(String tab, String keyword) {
        expireOverdueReservations();

        String normalizedTab = normalizeStaffTab(tab);
        String trimmedKeyword = keyword == null ? "" : keyword.trim();
        String lowerKeyword = trimmedKeyword.toLowerCase(Locale.ROOT);
        LocalDateTime now = LocalDateTime.now();

        List<Reservation> all = reservationRepository.findAllByOrderByReservedAtDescIdDesc();
        long pendingCount = all.stream().filter(r -> r.getStatus() == ReservationStatus.PENDING).count();
        long readyCount = all.stream().filter(r -> r.getStatus() == ReservationStatus.READY).count();
        long overdueHoldCount = all.stream()
                .filter(r -> r.getStatus() == ReservationStatus.READY
                        && r.getPickupUntil() != null
                        && r.getPickupUntil().isBefore(now))
                .count();
        long closedCount = all.stream().filter(r -> CLOSED_RESERVATION_STATUSES.contains(r.getStatus())).count();

        Map<Long, Long> availableCopiesByBook = new HashMap<>();
        Map<Long, Long> totalCopiesByBook = new HashMap<>();
        Map<Long, Integer> pendingTotalsByBook = new HashMap<>();
        Map<Long, Integer> pendingPositionByResId = new HashMap<>();

        Map<Long, List<Reservation>> pendingByBook = all.stream()
                .filter(r -> r.getStatus() == ReservationStatus.PENDING)
                .collect(Collectors.groupingBy(r -> r.getBook().getId()));
        for (Map.Entry<Long, List<Reservation>> entry : pendingByBook.entrySet()) {
            List<Reservation> queue = entry.getValue().stream()
                    .sorted(Comparator.comparing(Reservation::getReservedAt, Comparator.nullsLast(Comparator.naturalOrder()))
                            .thenComparing(Reservation::getId))
                    .toList();
            pendingTotalsByBook.put(entry.getKey(), queue.size());
            for (int i = 0; i < queue.size(); i++) {
                pendingPositionByResId.put(queue.get(i).getId(), i + 1);
            }
        }

        List<Reservation> searchMatched = all.stream()
                .filter(r -> matchesKeyword(r, lowerKeyword))
                .toList();

        List<Reservation> tabFiltered = switch (normalizedTab) {
            case "ready" -> searchMatched.stream()
                    .filter(r -> r.getStatus() == ReservationStatus.READY)
                    .sorted(Comparator.comparing(Reservation::getPickupUntil, Comparator.nullsLast(Comparator.naturalOrder())))
                    .toList();
            case "history" -> searchMatched.stream()
                    .filter(r -> CLOSED_RESERVATION_STATUSES.contains(r.getStatus()))
                    .toList();
            case "queues", "all" -> searchMatched.stream()
                    .filter(r -> normalizedTab.equals("all") || ACTIVE_RESERVATION_STATUSES.contains(r.getStatus()))
                    .toList();
            default -> searchMatched.stream()
                    .filter(r -> r.getStatus() == ReservationStatus.PENDING)
                    .sorted(Comparator.comparing(Reservation::getReservedAt, Comparator.nullsLast(Comparator.naturalOrder()))
                            .thenComparing(Reservation::getId))
                    .toList();
        };

        List<LibrarianReservationsView.StaffReservationRow> rows = tabFiltered.stream()
                .map(r -> toStaffRow(r, now, availableCopiesByBook, pendingPositionByResId, pendingTotalsByBook))
                .toList();

        Map<Long, List<Reservation>> activeByBook = searchMatched.stream()
                .filter(r -> ACTIVE_RESERVATION_STATUSES.contains(r.getStatus()))
                .collect(Collectors.groupingBy(r -> r.getBook().getId(), LinkedHashMap::new, Collectors.toList()));

        List<LibrarianReservationsView.BookQueueGroup> bookQueues = activeByBook.values().stream()
                .map(list -> {
                    Book book = list.get(0).getBook();
                    long avail = availableCopiesByBook.computeIfAbsent(book.getId(),
                            id -> bookCopyRepository.countByBook_IdAndStatus(id, CopyStatus.AVAILABLE));
                    long totalCopies = totalCopiesByBook.computeIfAbsent(book.getId(),
                            id -> bookCopyRepository.countByBook_Id(id));
                    List<Reservation> ordered = list.stream()
                            .sorted(Comparator
                                    .comparing((Reservation r) -> r.getStatus() == ReservationStatus.READY ? 0 : 1)
                                    .thenComparing(Reservation::getReservedAt, Comparator.nullsLast(Comparator.naturalOrder()))
                                    .thenComparing(Reservation::getId))
                            .toList();
                    long pCount = ordered.stream().filter(r -> r.getStatus() == ReservationStatus.PENDING).count();
                    long rCount = ordered.stream().filter(r -> r.getStatus() == ReservationStatus.READY).count();
                    List<LibrarianReservationsView.StaffReservationRow> groupRows = ordered.stream()
                            .map(r -> toStaffRow(r, now, availableCopiesByBook, pendingPositionByResId, pendingTotalsByBook))
                            .toList();
                    return new LibrarianReservationsView.BookQueueGroup(
                            book.getId(),
                            book.getBookCode(),
                            book.getTitle(),
                            avail,
                            totalCopies,
                            pCount,
                            rCount,
                            groupRows
                    );
                })
                .toList();

        return new LibrarianReservationsView(
                pendingCount,
                readyCount,
                overdueHoldCount,
                closedCount,
                normalizedTab,
                trimmedKeyword,
                rows,
                bookQueues
        );
    }

    @Override
    @Transactional
    public Reservation assignCopyAndMarkReady(Long reservationId, String librarianEmail) {
        Reservation locked = reservationRepository.findLockedById(reservationId)
                .orElseThrow(() -> new ReservationException(HttpStatus.NOT_FOUND, "Reservation not found."));
        if (locked.getStatus() != ReservationStatus.PENDING) {
            throw new ReservationException(HttpStatus.BAD_REQUEST,
                    "Only pending reservations can be assigned an available copy.");
        }

        Book book = locked.getBook();
        if (book.isDeleted() || book.getPublicationStatus() == PublicationStatus.DISCONTINUED) {
            throw new ReservationException(HttpStatus.BAD_REQUEST,
                    "Cannot assign copies for a deleted or discontinued book.");
        }

        BookCopy availableCopy = bookCopyRepository.findFirstByBook_IdAndStatus(book.getId(), CopyStatus.AVAILABLE)
                .orElseThrow(() -> new ReservationException(HttpStatus.BAD_REQUEST,
                        "No available copy is currently on the shelf for this book."));

        LocalDateTime now = LocalDateTime.now();
        BookCopy heldCopy = bookInventoryService.markReserved(availableCopy.getId());
        locked.setStatus(ReservationStatus.READY);
        locked.setAssignedCopy(heldCopy);
        locked.setReadyAt(now);
        locked.setPickupUntil(now.plusDays(HOLD_DURATION_DAYS));
        if (librarianEmail != null && !librarianEmail.isBlank()
                && (locked.getNotes() == null || locked.getNotes().isBlank())) {
            locked.setNotes("Copy " + heldCopy.getBarcode() + " held by " + librarianEmail);
        }

        Reservation saved = reservationRepository.saveAndFlush(locked);
        // Initialize lazy associations needed by post-transaction controller flash messages.
        saved.getMember().getFullName();
        saved.getBook().getTitle();
        heldCopy.getBarcode();
        notificationService.notifyReservationReady(saved);
        return saved;
    }

    @Override
    @Transactional
    public Loan fulfillReservation(Long reservationId, String librarianEmail) {
        Reservation locked = reservationRepository.findLockedById(reservationId)
                .orElseThrow(() -> new ReservationException(HttpStatus.NOT_FOUND, "Reservation not found."));
        if (locked.getStatus() != ReservationStatus.READY) {
            throw new ReservationException(HttpStatus.BAD_REQUEST,
                    "Only reservations ready for pickup can be fulfilled into a loan.");
        }

        LocalDateTime now = LocalDateTime.now();
        if (locked.getPickupUntil() != null && !locked.getPickupUntil().isAfter(now)) {
            throw new ReservationException(HttpStatus.BAD_REQUEST,
                    "This pickup hold has expired. Refresh or process expired holds first.");
        }

        BookCopy copy = locked.getAssignedCopy();
        if (copy == null) {
            throw new ReservationException(HttpStatus.BAD_REQUEST,
                    "This reservation does not have a held copy assigned.");
        }

        User member = locked.getMember();
        if (!memberEligibilityService.isEligible(member.getId())) {
            throw new ReservationException(HttpStatus.BAD_REQUEST,
                    "Member account or library card is not active. Cannot hand over book.");
        }
        if (fineRepository.existsByMember_IdAndStatus(member.getId(), FineStatus.UNPAID)) {
            throw new ReservationException(HttpStatus.BAD_REQUEST,
                    "Member has unpaid fines. Please settle fines before pickup handover.");
        }
        long activeLoanCount = loanRepository.countByMember_IdAndStatusIn(member.getId(), ACTIVE_LOAN_STATUSES);
        if (activeLoanCount >= MAX_LOAN_LIMIT) {
            throw new ReservationException(HttpStatus.BAD_REQUEST,
                    "Member has already reached the maximum of " + MAX_LOAN_LIMIT + " active loans.");
        }

        User librarian = (librarianEmail != null && !librarianEmail.isBlank())
                ? userRepository.findByEmail(librarianEmail).orElse(null)
                : null;

        BookCopy borrowedCopy = bookInventoryService.markBorrowed(copy.getId());
        LocalDate dueDate = LocalDate.now().plusDays(STANDARD_LOAN_DAYS);

        Loan loan = loanRepository.saveAndFlush(Loan.builder()
                .copy(borrowedCopy)
                .member(member)
                .issuedBy(librarian)
                .borrowedAt(now)
                .dueDate(dueDate)
                .status(LoanStatus.BORROWING)
                .renewCount(0)
                .note("Fulfilled from reservation " + locked.displayCode())
                .build());

        loanItemRepository.saveAndFlush(LoanItem.builder()
                .loan(loan)
                .copy(borrowedCopy)
                .dueDate(dueDate)
                .status(LoanItemStatus.BORROWING)
                .renewCount(0)
                .build());

        locked.setStatus(ReservationStatus.FULFILLED);
        locked.setFulfilledAt(now);
        locked.setNotes("Picked up copy " + borrowedCopy.getBarcode() + " on Loan #" + loan.getId());
        Reservation savedResv = reservationRepository.saveAndFlush(locked);
        member.getFullName();
        savedResv.getBook().getTitle();
        notificationService.notifyReservationFulfilled(savedResv);

        log.info("Fulfilled reservation {} into loan {} (copy {}) by {}",
                savedResv.displayCode(), loan.getId(), borrowedCopy.getBarcode(), librarianEmail);
        return loan;
    }

    @Override
    @Transactional
    public void cancelReservationByStaff(Long reservationId, String reason, String librarianEmail) {
        Reservation locked = reservationRepository.findLockedById(reservationId)
                .orElseThrow(() -> new ReservationException(HttpStatus.NOT_FOUND, "Reservation not found."));
        if (!ACTIVE_RESERVATION_STATUSES.contains(locked.getStatus())) {
            throw new ReservationException(HttpStatus.BAD_REQUEST, "Only active reservations can be canceled.");
        }

        LocalDateTime now = LocalDateTime.now();
        BookCopy heldCopy = locked.getStatus() == ReservationStatus.READY ? locked.getAssignedCopy() : null;
        String cleanReason = (reason != null && !reason.isBlank())
                ? reason.trim()
                : "Canceled by circulation staff" + (librarianEmail != null ? " (" + librarianEmail + ")" : "");

        locked.setStatus(ReservationStatus.CANCELLED);
        locked.setCancelledAt(now);
        locked.setAssignedCopy(null);
        locked.setNotes(cleanReason);
        Reservation saved = reservationRepository.saveAndFlush(locked);
        notificationService.notifyReservationCancelled(saved, true);

        if (heldCopy != null) {
            assignCopyToNextPendingReservation(heldCopy, now);
        }
    }

    private void releaseCopyBackToShelf(Long copyId, CopyStatus currentStatus) {
        if (currentStatus == CopyStatus.BORROWED) {
            bookInventoryService.markReturned(copyId);
        } else if (currentStatus == CopyStatus.RESERVED) {
            bookInventoryService.releaseReservation(copyId);
        }
    }

    private User findUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ReservationException(HttpStatus.NOT_FOUND, "User account not found."));
    }

    private MemberReservationsView.ReservationCardDto toMemberCardDto(Reservation r) {
        Book book = r.getBook();
        String authors = book.getAuthors().stream()
                .map(Author::getName)
                .sorted()
                .collect(Collectors.joining(", "));
        if (authors.isBlank()) {
            authors = "Unknown author";
        }
        String category = book.getCategories().stream()
                .map(Category::getName)
                .sorted()
                .collect(Collectors.joining(", "));
        if (category.isBlank()) {
            category = "General collection";
        }
        String cover = (book.getCoverUrl() != null && !book.getCoverUrl().isBlank())
                ? book.getCoverUrl()
                : DEFAULT_COVER;

        int queuePos = 0;
        int totalInQueue = 0;
        LocalDate estimatedDate = null;
        String heldBarcode = null;
        String pickupLocation = DEFAULT_PICKUP_LOCATION;

        if (r.getStatus() == ReservationStatus.READY) {
            if (r.getAssignedCopy() != null) {
                heldBarcode = r.getAssignedCopy().getBarcode();
                if (r.getAssignedCopy().getShelfLocation() != null && !r.getAssignedCopy().getShelfLocation().isBlank()) {
                    pickupLocation = DEFAULT_PICKUP_LOCATION + " · " + r.getAssignedCopy().getShelfLocation();
                }
            }
        } else if (r.getStatus() == ReservationStatus.PENDING) {
            long ahead = r.getReservedAt() != null && r.getId() != null
                    ? reservationRepository.countAheadInQueue(book.getId(), r.getReservedAt(), r.getId())
                    : 0L;
            queuePos = (int) ahead + 1;
            totalInQueue = Math.max(queuePos, (int) reservationRepository.countByBook_IdAndStatus(book.getId(), ReservationStatus.PENDING));
            estimatedDate = estimateAvailabilityDate(book.getId(), queuePos);
        } else if (r.getAssignedCopy() != null) {
            heldBarcode = r.getAssignedCopy().getBarcode();
        }

        LocalDateTime closedAt = switch (r.getStatus()) {
            case FULFILLED -> r.getFulfilledAt();
            case CANCELLED -> r.getCancelledAt();
            case EXPIRED -> r.getExpiredAt();
            default -> null;
        };

        boolean canCancel = r.getStatus() == ReservationStatus.PENDING || r.getStatus() == ReservationStatus.READY;

        return new MemberReservationsView.ReservationCardDto(
                r.getId(),
                r.displayCode(),
                book.getId(),
                book.getBookCode(),
                book.getTitle(),
                authors,
                category,
                cover,
                r.getStatus(),
                r.getReservedAt(),
                r.getReadyAt(),
                r.getPickupUntil(),
                heldBarcode,
                pickupLocation,
                queuePos,
                totalInQueue,
                estimatedDate,
                closedAt,
                r.getNotes(),
                canCancel
        );
    }

    private LocalDate estimateAvailabilityDate(Long bookId, int queuePosition) {
        LocalDate today = LocalDate.now();
        List<Loan> activeLoans = loanRepository.findByCopy_Book_IdAndStatusInOrderByDueDateAsc(bookId, ACTIVE_LOAN_STATUSES);
        if (!activeLoans.isEmpty()) {
            int slotIndex = Math.min(queuePosition - 1, activeLoans.size() - 1);
            int extraCycles = Math.max(0, (queuePosition - 1) / activeLoans.size());
            LocalDate base = activeLoans.get(slotIndex).getDueDate();
            if (base == null || base.isBefore(today)) {
                base = today.plusDays(2);
            }
            return base.plusDays((long) extraCycles * STANDARD_LOAN_DAYS);
        }
        return today.plusDays((long) Math.max(1, queuePosition) * HOLD_DURATION_DAYS);
    }

    private LibrarianReservationsView.StaffReservationRow toStaffRow(
            Reservation r,
            LocalDateTime now,
            Map<Long, Long> availableCopiesByBook,
            Map<Long, Integer> pendingPositionByResId,
            Map<Long, Integer> pendingTotalsByBook
    ) {
        Book book = r.getBook();
        User member = r.getMember();
        long available = availableCopiesByBook.computeIfAbsent(book.getId(),
                id -> bookCopyRepository.countByBook_IdAndStatus(id, CopyStatus.AVAILABLE));

        BookCopy assignedCopy = r.getAssignedCopy();
        String barcode = assignedCopy != null ? assignedCopy.getBarcode() : null;
        String shelf = assignedCopy != null && assignedCopy.getShelfLocation() != null
                ? assignedCopy.getShelfLocation()
                : DEFAULT_PICKUP_LOCATION;

        int pos = r.getStatus() == ReservationStatus.PENDING
                ? pendingPositionByResId.getOrDefault(r.getId(), 1)
                : 0;
        int total = r.getStatus() == ReservationStatus.PENDING
                ? pendingTotalsByBook.getOrDefault(book.getId(), 1)
                : 0;

        boolean expired = r.getStatus() == ReservationStatus.READY
                && r.getPickupUntil() != null
                && !r.getPickupUntil().isAfter(now);

        LocalDateTime closedAt = switch (r.getStatus()) {
            case FULFILLED -> r.getFulfilledAt();
            case CANCELLED -> r.getCancelledAt();
            case EXPIRED -> r.getExpiredAt();
            default -> null;
        };

        return new LibrarianReservationsView.StaffReservationRow(
                r.getId(),
                r.displayCode(),
                r.getStatus(),
                book.getId(),
                book.getBookCode(),
                book.getTitle(),
                available,
                member.getId(),
                member.getFullName(),
                member.getEmail(),
                member.getMemberCode(),
                r.getReservedAt(),
                r.getReadyAt(),
                r.getPickupUntil(),
                expired,
                barcode,
                shelf,
                pos,
                total,
                r.getNotes(),
                closedAt
        );
    }

    private boolean matchesKeyword(Reservation r, String lowerKeyword) {
        if (lowerKeyword.isEmpty()) {
            return true;
        }
        return containsIgnoreCase(r.displayCode(), lowerKeyword)
                || containsIgnoreCase(r.getBook().getTitle(), lowerKeyword)
                || containsIgnoreCase(r.getBook().getBookCode(), lowerKeyword)
                || containsIgnoreCase(r.getMember().getFullName(), lowerKeyword)
                || containsIgnoreCase(r.getMember().getEmail(), lowerKeyword)
                || containsIgnoreCase(r.getMember().getMemberCode(), lowerKeyword)
                || (r.getAssignedCopy() != null && containsIgnoreCase(r.getAssignedCopy().getBarcode(), lowerKeyword));
    }

    private boolean containsIgnoreCase(String value, String lowerKeyword) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(lowerKeyword);
    }

    private String normalizeStaffTab(String tab) {
        if (tab == null || tab.isBlank()) {
            return "pending";
        }
        return switch (tab.trim().toLowerCase(Locale.ROOT)) {
            case "ready", "queues", "history", "all", "pending" -> tab.trim().toLowerCase(Locale.ROOT);
            default -> "pending";
        };
    }
}
