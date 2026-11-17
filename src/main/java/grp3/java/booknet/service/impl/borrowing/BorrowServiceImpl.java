package grp3.java.booknet.service.impl.borrowing;

import grp3.java.booknet.dto.borrowing.BorrowConfirmView;
import grp3.java.booknet.dto.borrowing.BorrowRequestForm;
import grp3.java.booknet.entity.book.Author;
import grp3.java.booknet.entity.book.Book;
import grp3.java.booknet.entity.book.BookCopy;
import grp3.java.booknet.entity.book_service.Reservation;
import grp3.java.booknet.entity.user.User;
import grp3.java.booknet.enums.book.CopyStatus;
import grp3.java.booknet.enums.book_service.FineStatus;
import grp3.java.booknet.enums.book_service.LoanStatus;
import grp3.java.booknet.enums.book_service.ReservationStatus;
import grp3.java.booknet.enums.user.UserRole;
import grp3.java.booknet.enums.user.UserStatus;
import grp3.java.booknet.exception.account.AccountException;
import grp3.java.booknet.repository.book.BookCopyRepository;
import grp3.java.booknet.repository.book.BookRepository;
import grp3.java.booknet.repository.book_service.FineRepository;
import grp3.java.booknet.repository.book_service.LoanRepository;
import grp3.java.booknet.repository.book_service.ReservationRepository;
import grp3.java.booknet.repository.user.UserRepository;
import grp3.java.booknet.service.borrowing.BorrowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BorrowServiceImpl implements BorrowService {

    public static final int MAX_LOAN_LIMIT = 5;
    public static final int LOAN_PERIOD_DAYS = 14;

    private static final Set<LoanStatus> ACTIVE_LOAN_STATUSES = Set.of(
            LoanStatus.BORROWING,
            LoanStatus.RENEW_PENDING,
            LoanStatus.RETURN_PENDING,
            LoanStatus.OVERDUE
    );

    private static final Set<ReservationStatus> ACTIVE_RESERVATION_STATUSES = Set.of(
            ReservationStatus.PENDING,
            ReservationStatus.READY
    );

    private final UserRepository userRepository;
    private final BookRepository bookRepository;
    private final BookCopyRepository bookCopyRepository;
    private final LoanRepository loanRepository;
    private final ReservationRepository reservationRepository;
    private final FineRepository fineRepository;

    @Override
    @Transactional(readOnly = true)
    public BorrowConfirmView prepareConfirmView(Long userId, List<Long> bookIds) {
        User user = validateAndGetUser(userId);

        if (bookIds == null || bookIds.isEmpty()) {
            throw new AccountException(HttpStatus.BAD_REQUEST, "No books selected for borrowing.");
        }

        List<Book> books = bookRepository.findAllById(bookIds);
        if (books.isEmpty()) {
            throw new AccountException(HttpStatus.NOT_FOUND, "Selected book(s) not found.");
        }

        int activeLoans = getActiveBorrowCount(userId);
        int remainingQuota = MAX_LOAN_LIMIT - activeLoans;

        String ineligibleReason = null;
        boolean eligible = true;

        if (fineRepository.existsByMember_IdAndStatus(userId, FineStatus.UNPAID)) {
            eligible = false;
            ineligibleReason = "Your account has unpaid library fines. Please clear fines before requesting loans.";
        } else if (remainingQuota <= 0) {
            eligible = false;
            ineligibleReason = "You have reached the maximum borrowing limit of " + MAX_LOAN_LIMIT + " books.";
        } else if (books.size() > remainingQuota) {
            eligible = false;
            ineligibleReason = "You can only request " + remainingQuota + " more book(s) (Limit: " + MAX_LOAN_LIMIT + ", currently active: " + activeLoans + ").";
        }

        LocalDate pickupDate = LocalDate.now();
        LocalDate returnDueDate = pickupDate.plusDays(LOAN_PERIOD_DAYS);

        List<BorrowConfirmView.SelectedBookItem> bookItems = books.stream().map(book -> {
            String authorNames = book.getAuthors().stream()
                    .map(Author::getName)
                    .collect(Collectors.joining(", "));
            if (authorNames.isBlank()) {
                authorNames = "Unknown author";
            }

            String publisherYear = "";
            if (book.getPublisher() != null) {
                publisherYear += book.getPublisher().getName();
            }
            if (book.getPublishYear() != null) {
                publisherYear += (publisherYear.isBlank() ? "" : ", ") + book.getPublishYear();
            }

            BookCopy availableCopy = bookCopyRepository.findFirstByBook_IdAndStatus(book.getId(), CopyStatus.AVAILABLE)
                    .orElse(null);
            String barcode = availableCopy != null ? availableCopy.getBarcode() : (book.getIsbn() != null ? book.getIsbn() : "N/A");
            String shelfLocation = availableCopy != null && availableCopy.getShelfLocation() != null
                    ? availableCopy.getShelfLocation()
                    : "Circulation desk · Floor 1";

            String cover = book.getCoverUrl() != null && !book.getCoverUrl().isBlank()
                    ? book.getCoverUrl()
                    : "/images/home/cover-hoa-vang.jpg";

            return new BorrowConfirmView.SelectedBookItem(
                    book.getId(),
                    book.getTitle(),
                    authorNames,
                    publisherYear,
                    barcode,
                    shelfLocation,
                    cover
            );
        }).toList();

        String memberCode = user.getMemberCode() != null && !user.getMemberCode().isBlank()
                ? user.getMemberCode()
                : "MB-" + String.format("%05d", user.getId());

        return new BorrowConfirmView(
                bookItems,
                user.getFullName(),
                memberCode,
                pickupDate,
                returnDueDate,
                activeLoans,
                MAX_LOAN_LIMIT,
                eligible,
                ineligibleReason
        );
    }

    @Override
    @Transactional
    public void submitBorrowRequest(Long userId, BorrowRequestForm form) {
        User user = validateAndGetUser(userId);

        if (!form.isTermsAccepted()) {
            throw new AccountException(HttpStatus.BAD_REQUEST, "You must accept the library terms of borrowing.");
        }

        List<Long> bookIds = form.getBookIds();
        if (bookIds == null || bookIds.isEmpty()) {
            throw new AccountException(HttpStatus.BAD_REQUEST, "No books selected for borrowing.");
        }

        if (fineRepository.existsByMember_IdAndStatus(userId, FineStatus.UNPAID)) {
            throw new AccountException(HttpStatus.BAD_REQUEST, "Your account has unpaid library fines.");
        }

        int activeLoans = getActiveBorrowCount(userId);
        if (activeLoans + bookIds.size() > MAX_LOAN_LIMIT) {
            throw new AccountException(HttpStatus.BAD_REQUEST,
                    "Borrowing limit exceeded. Maximum allowed: " + MAX_LOAN_LIMIT + ", currently active: " + activeLoans);
        }

        List<Book> books = bookRepository.findAllById(bookIds);
        if (books.size() != bookIds.size()) {
            throw new AccountException(HttpStatus.NOT_FOUND, "One or more requested books could not be found.");
        }

        List<Reservation> newReservations = new ArrayList<>();
        for (Book book : books) {
            if (reservationRepository.existsByMember_IdAndBook_IdAndStatusIn(userId, book.getId(), ACTIVE_RESERVATION_STATUSES)) {
                throw new AccountException(HttpStatus.CONFLICT, "You already have an active request for '" + book.getTitle() + "'.");
            }

            Reservation reservation = Reservation.builder()
                    .member(user)
                    .book(book)
                    .status(ReservationStatus.PENDING)
                    .build();
            newReservations.add(reservation);
        }

        reservationRepository.saveAll(newReservations);
        log.info("Member {} requested {} book(s) under reservation IDs: {}",
                user.getId(), newReservations.size(),
                newReservations.stream().map(Reservation::getId).toList());
    }

    private int getActiveBorrowCount(Long userId) {
        long currentLoans = loanRepository.countByMember_IdAndStatusIn(userId, ACTIVE_LOAN_STATUSES);
        long activeReservations = reservationRepository.countByMember_IdAndStatusIn(userId, ACTIVE_RESERVATION_STATUSES);
        return (int) (currentLoans + activeReservations);
    }

    private User validateAndGetUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AccountException(HttpStatus.UNAUTHORIZED, "User account not found."));

        if (user.getRole() != UserRole.ROLE_USER && user.getRole() != UserRole.ROLE_ADMIN) {
            throw new AccountException(HttpStatus.FORBIDDEN, "Access denied: Only registered members can borrow books.");
        }

        if (!user.isEnabled() || !user.isEmailVerified() || user.getStatus() != UserStatus.ACTIVE) {
            throw new AccountException(HttpStatus.FORBIDDEN, "Your membership card is unverified, inactive or suspended.");
        }

        return user;
    }
}
