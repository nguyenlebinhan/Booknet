package grp3.java.booknet.service.impl.book;

import grp3.java.booknet.dto.book.request.BookCopyForm;
import grp3.java.booknet.dto.book.response.InventoryStatsDto;
import grp3.java.booknet.entity.book.Book;
import grp3.java.booknet.entity.book.BookCopy;
import grp3.java.booknet.enums.book.CopyStatus;
import grp3.java.booknet.enums.book.PublicationStatus;
import grp3.java.booknet.repository.book.BookCopyRepository;
import grp3.java.booknet.repository.book.BookRepository;
import grp3.java.booknet.service.book.BookCopyService;
import grp3.java.booknet.service.book.BookInventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BookCopyServiceImpl implements BookCopyService, BookInventoryService {

    private final BookCopyRepository bookCopyRepository;
    private final BookRepository bookRepository;

    @Override
    @Transactional
    public BookCopy createCopy(Long bookId, BookCopyForm form) {
        Book book = getManageableBook(bookId);
        String barcode = normalizeBarcode(form.getBarcode());

        if (barcode == null) {
            barcode = generateBarcode(book);
        } else if (bookCopyRepository.existsByBarcode(barcode)) {
            throw new IllegalArgumentException("Barcode '" + barcode + "' already exists.");
        }

        BookCopy copy = BookCopy.builder()
                .book(book)
                .barcode(barcode)
                .shelfLocation(trimToNull(form.getShelfLocation()))
                .condition(trimToNull(form.getCondition()))
                .acquiredDate(form.getAcquiredDate())
                .status(CopyStatus.AVAILABLE)
                .build();

        return bookCopyRepository.save(copy);
    }

    @Override
    @Transactional
    public BookCopy updateCopy(Long copyId, BookCopyForm form) {
        BookCopy copy = bookCopyRepository.findByIdForUpdate(copyId)
                .orElseThrow(() -> new IllegalArgumentException("Book copy not found: " + copyId));

        String barcode = normalizeBarcode(form.getBarcode());
        if (barcode == null) {
            throw new IllegalArgumentException("Barcode is required when updating a copy.");
        }
        if (bookCopyRepository.existsByBarcodeAndIdNot(barcode, copyId)) {
            throw new IllegalArgumentException("Barcode '" + barcode + "' is already used by another copy.");
        }

        copy.setBarcode(barcode);
        copy.setShelfLocation(trimToNull(form.getShelfLocation()));
        copy.setCondition(trimToNull(form.getCondition()));
        copy.setAcquiredDate(form.getAcquiredDate());

        return copy;
    }

    @Override
    @Transactional(readOnly = true)
    public BookCopy getCopy(Long copyId) {
        return bookCopyRepository.findByIdWithBook(copyId)
                .orElseThrow(() -> new IllegalArgumentException("Book copy not found: " + copyId));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BookCopy> getCopies(Long bookId, Pageable pageable) {
        getBookManageableOrThrow(bookId);
        return bookCopyRepository.findByBook_Id(bookId, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookCopy> getAvailableCopies(Long bookId) {
        Book book = getBookManageableOrThrow(bookId);
        if (book.getPublicationStatus() == PublicationStatus.DISCONTINUED || book.isDeleted()) {
            return List.of();
        }
        return bookCopyRepository.findByBook_IdAndStatus(bookId, CopyStatus.AVAILABLE);
    }

    @Override
    @Transactional(readOnly = true)
    public InventoryStatsDto getInventoryStats(Long bookId) {
        Book book = getBookManageableOrThrow(bookId);
        long available = count(bookId, CopyStatus.AVAILABLE);
        long borrowed = count(bookId, CopyStatus.BORROWED);
        long reserved = count(bookId, CopyStatus.RESERVED);
        long damaged = count(bookId, CopyStatus.DAMAGED);
        long discontinued = count(bookId, CopyStatus.DISCONTINUED);
        long lost = count(bookId, CopyStatus.LOST);
        long total = available + borrowed + reserved + damaged + discontinued + lost;

        return InventoryStatsDto.builder()
                .bookId(book.getId())
                .bookTitle(book.getTitle())
                .bookCode(book.getBookCode())
                .totalCopies(total)
                .availableCopies(available)
                .borrowedCopies(borrowed)
                .reservedCopies(reserved)
                .damagedCopies(damaged)
                .discontinuedCopies(discontinued)
                .lostCopies(lost)
                .build();
    }

    // Inventory boundary used by other modules. All status changes are serialized
    // by a database row lock and committed in one transaction.
    @Override
    @Transactional
    public BookCopy changeCopyStatus(Long copyId, CopyStatus targetStatus) {
        if (targetStatus == null) {
            throw new IllegalArgumentException("Target status is required.");
        }
        BookCopy copy = lockCopy(copyId);
        ensureBookAllowsCirculation(copy, targetStatus);

        if (copy.getStatus() == targetStatus) {
            return copy;
        }
        if (!isAllowed(copy.getStatus(), targetStatus)) {
            throw new IllegalStateException(
                    "Cannot change copy status from " + copy.getStatus() + " to " + targetStatus + ".");
        }
        copy.setStatus(targetStatus);
        return copy;
    }

    @Override
    @Transactional
    public BookCopy markBorrowed(Long copyId) {
        BookCopy copy = lockCopy(copyId);
        ensureBookAllowsCirculation(copy, CopyStatus.BORROWED);
        if (copy.getStatus() != CopyStatus.AVAILABLE && copy.getStatus() != CopyStatus.RESERVED) {
            throw new IllegalStateException("Only an AVAILABLE or RESERVED copy can be borrowed.");
        }
        copy.setStatus(CopyStatus.BORROWED);
        return copy;
    }

    @Override
    @Transactional
    public BookCopy markReserved(Long copyId) {
        BookCopy copy = lockCopy(copyId);
        ensureBookAllowsCirculation(copy, CopyStatus.RESERVED);
        if (copy.getStatus() != CopyStatus.AVAILABLE) {
            throw new IllegalStateException("Only an AVAILABLE copy can be reserved.");
        }
        copy.setStatus(CopyStatus.RESERVED);
        return copy;
    }

    @Override
    @Transactional
    public BookCopy releaseReservation(Long copyId) {
        BookCopy copy = lockCopy(copyId);
        if (copy.getStatus() != CopyStatus.RESERVED) {
            throw new IllegalStateException("Only a RESERVED copy can be released.");
        }
        copy.setStatus(copy.getBook().getPublicationStatus() == PublicationStatus.DISCONTINUED
                ? CopyStatus.DISCONTINUED : CopyStatus.AVAILABLE);
        return copy;
    }

    @Override
    @Transactional
    public BookCopy markReturned(Long copyId) {
        BookCopy copy = lockCopy(copyId);
        if (copy.getStatus() != CopyStatus.BORROWED) {
            throw new IllegalStateException("Only a BORROWED copy can be returned.");
        }
        copy.setStatus(copy.getBook().getPublicationStatus() == PublicationStatus.DISCONTINUED
                ? CopyStatus.DISCONTINUED : CopyStatus.AVAILABLE);
        return copy;
    }

    private BookCopy lockCopy(Long copyId) {
        return bookCopyRepository.findByIdForUpdate(copyId)
                .orElseThrow(() -> new IllegalArgumentException("Book copy not found: " + copyId));
    }

    private boolean isAllowed(CopyStatus from, CopyStatus to) {
        return switch (from) {
            case AVAILABLE -> to == CopyStatus.BORROWED || to == CopyStatus.RESERVED
                    || to == CopyStatus.DAMAGED || to == CopyStatus.LOST
                    || to == CopyStatus.DISCONTINUED;
            case RESERVED -> to == CopyStatus.AVAILABLE || to == CopyStatus.BORROWED;
            case BORROWED -> to == CopyStatus.AVAILABLE || to == CopyStatus.DAMAGED || to == CopyStatus.LOST;
            case DAMAGED -> to == CopyStatus.AVAILABLE || to == CopyStatus.LOST
                    || to == CopyStatus.DISCONTINUED;
            case LOST -> to == CopyStatus.AVAILABLE || to == CopyStatus.DISCONTINUED;
            case DISCONTINUED -> to == CopyStatus.AVAILABLE;
        };
    }

    private Book getManageableBook(Long bookId) {
        Book book = getBookManageableOrThrow(bookId);
        if (book.isDeleted()) {
            throw new IllegalArgumentException("Cannot add copies to a deleted book.");
        }
        if (book.getPublicationStatus() == PublicationStatus.DISCONTINUED) {
            throw new IllegalArgumentException("Cannot add copies to a discontinued book.");
        }
        return book;
    }

    private Book getBookManageableOrThrow(Long bookId) {
        return bookRepository.findById(bookId)
                .orElseThrow(() -> new IllegalArgumentException("Book not found: " + bookId));
    }


    private void ensureBookAllowsCirculation(BookCopy copy, CopyStatus targetStatus) {
        if ((targetStatus == CopyStatus.BORROWED || targetStatus == CopyStatus.RESERVED)
                && (copy.getBook().isDeleted()
                || copy.getBook().getPublicationStatus() == PublicationStatus.DISCONTINUED)) {
            throw new IllegalStateException("A discontinued book cannot be newly borrowed or reserved.");
        }
    }

    private long count(Long bookId, CopyStatus status) {
        return bookCopyRepository.countByBook_IdAndStatus(bookId, status);
    }

    private String normalizeBarcode(String barcode) {
        if (barcode == null || barcode.isBlank()) {
            return null;
        }
        String normalized = barcode.trim().toUpperCase(Locale.ROOT);
        if (normalized.length() > 40) {
            throw new IllegalArgumentException("Barcode must not exceed 40 characters.");
        }
        return normalized;
    }

    private String generateBarcode(Book book) {
        String prefix = book.getBookCode() == null || book.getBookCode().isBlank()
                ? "BOOK-" + book.getId()
                : book.getBookCode().trim().toUpperCase(Locale.ROOT);
        prefix = prefix.replaceAll("[^A-Z0-9-]", "-");
        String barcode;
        do {
            String suffix = UUID.randomUUID().toString().replace("-", "")
                    .substring(0, 12).toUpperCase(Locale.ROOT);
            String maxPrefix = prefix.substring(0, Math.min(prefix.length(), 27));
            barcode = maxPrefix + "-" + suffix;
        } while (bookCopyRepository.existsByBarcode(barcode));
        return barcode;
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
