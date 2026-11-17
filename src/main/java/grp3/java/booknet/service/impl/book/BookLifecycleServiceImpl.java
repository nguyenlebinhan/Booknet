package grp3.java.booknet.service.impl.book;

import grp3.java.booknet.dto.book.response.BookDeletionCheckDto;
import grp3.java.booknet.entity.book.Book;
import grp3.java.booknet.enums.book.PublicationStatus;
import grp3.java.booknet.repository.book.BookRepository;
import grp3.java.booknet.service.book.BookLifecycleService;
import grp3.java.booknet.service.book.BookReferenceGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BookLifecycleServiceImpl implements BookLifecycleService {
    private final BookRepository bookRepository;
    private final BookReferenceGuard referenceGuard;

    @Override
    @Transactional(readOnly = true)
    public BookDeletionCheckDto checkHardDelete(Long bookId) {
        Book book = getBook(bookId);
        List<String> blockers = referenceGuard.findBlockingReferences(bookId);
        return toCheck(book, blockers);
    }

    @Override
    @Transactional
    public void hardDelete(Long bookId, String confirmationCode) {
        Book book = getBookForUpdate(bookId);
        String expected = book.getBookCode() == null ? "" : book.getBookCode().trim();
        String supplied = confirmationCode == null ? "" : confirmationCode.trim();
        if (expected.isBlank() || !expected.equals(supplied)) {
            throw new IllegalArgumentException("Confirmation code does not match the book code.");
        }

        List<String> blockers = referenceGuard.findBlockingReferences(bookId);
        if (!blockers.isEmpty()) {
            throw new IllegalStateException("Hard delete is blocked because historical or foreign-key references exist: "
                    + String.join(", ", blockers) + ". Discontinue the book instead.");
        }

        try {
            // Book.copies is cascade=ALL/orphanRemoval; shared Author/Category entities are not cascaded.
            bookRepository.delete(book);
            bookRepository.flush();
        } catch (DataIntegrityViolationException ex) {
            // A concurrent insert may have created a reference after the pre-check.
            throw new IllegalStateException(
                    "The book acquired a database reference during deletion. No historical data was deleted; discontinue it instead.", ex);
        }
    }

    @Override
    @Transactional
    public void discontinue(Long bookId) {
        Book book = getBookForUpdate(bookId);
        if (book.isDeleted()) {
            throw new IllegalStateException("A deleted book cannot be discontinued.");
        }
        if (book.getPublicationStatus() == PublicationStatus.DISCONTINUED) return;
        book.setPublicationStatus(PublicationStatus.DISCONTINUED);
        // Keep deleted=false so administrators can still find and audit the book.
    }

    private Book getBook(Long id) {
        return bookRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Book not found with ID: " + id));
    }

    private Book getBookForUpdate(Long id) {
        return bookRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new IllegalArgumentException("Book not found with ID: " + id));
    }

    private BookDeletionCheckDto toCheck(Book book, List<String> blockers) {
        return BookDeletionCheckDto.builder()
                .bookId(book.getId())
                .bookCode(book.getBookCode())
                .title(book.getTitle())
                .hardDeleteAllowed(blockers.isEmpty())
                .blockingReferences(blockers)
                .build();
    }
}
