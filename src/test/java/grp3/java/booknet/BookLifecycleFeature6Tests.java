package grp3.java.booknet;

import grp3.java.booknet.entity.book.Book;
import grp3.java.booknet.entity.book.BookCopy;
import grp3.java.booknet.enums.book.CopyStatus;
import grp3.java.booknet.enums.book.PublicationStatus;
import grp3.java.booknet.repository.book.BookCopyRepository;
import grp3.java.booknet.repository.book.BookRepository;
import grp3.java.booknet.service.BookReferenceGuard;
import grp3.java.booknet.service.impl.BookCopyServiceImpl;
import grp3.java.booknet.service.impl.BookLifecycleServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookLifecycleFeature6Tests {

    @Mock private BookRepository bookRepository;
    @Mock private BookReferenceGuard referenceGuard;
    @Mock private BookCopyRepository bookCopyRepository;

    @InjectMocks private BookLifecycleServiceImpl lifecycleService;
    @InjectMocks private BookCopyServiceImpl bookCopyService;

    private Book book;

    @BeforeEach
    void setUp() {
        book = Book.builder()
                .title("Effective Java")
                .bookCode("BK-EJ-01")
                .publicationStatus(PublicationStatus.PUBLISHED)
                .deleted(false)
                .build();
        book.setId(10L);
    }

    @Test
    @DisplayName("Hard delete succeeds only when confirmation code matches and no references exist")
    void hardDeleteWhenSafe() {
        when(bookRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(book));
        when(referenceGuard.findBlockingReferences(10L)).thenReturn(List.of());

        lifecycleService.hardDelete(10L, "BK-EJ-01");

        verify(bookRepository).delete(book);
        verify(bookRepository).flush();
    }

    @Test
    @DisplayName("Hard delete is blocked when historical foreign-key references exist")
    void hardDeleteBlockedByHistory() {
        when(bookRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(book));
        when(referenceGuard.findBlockingReferences(10L)).thenReturn(List.of("loans.copy_id"));

        assertThatThrownBy(() -> lifecycleService.hardDelete(10L, "BK-EJ-01"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("historical or foreign-key references");

        verify(bookRepository, never()).delete(any(Book.class));
        verify(bookRepository, never()).flush();
    }

    @Test
    @DisplayName("Hard delete requires exact book-code confirmation")
    void hardDeleteRequiresConfirmation() {
        when(bookRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(book));

        assertThatThrownBy(() -> lifecycleService.hardDelete(10L, "BK-EJ-WRONG"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("does not match");

        verifyNoInteractions(referenceGuard);
        verify(bookRepository, never()).delete(any(Book.class));
    }

    @Test
    @DisplayName("Discontinue changes book status but preserves deleted flag")
    void discontinuePreservesBook() {
        when(bookRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(book));

        lifecycleService.discontinue(10L);

        assertThat(book.getPublicationStatus()).isEqualTo(PublicationStatus.DISCONTINUED);
        assertThat(book.isDeleted()).isFalse();
        verify(bookRepository, never()).delete(any(Book.class));
    }

    @Test
    @DisplayName("Discontinuing an already discontinued book is idempotent")
    void discontinueIsIdempotent() {
        book.setPublicationStatus(PublicationStatus.DISCONTINUED);
        when(bookRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(book));

        lifecycleService.discontinue(10L);

        assertThat(book.getPublicationStatus()).isEqualTo(PublicationStatus.DISCONTINUED);
    }

    @Test
    @DisplayName("Returning a copy of a discontinued book keeps it out of circulation")
    void returnedCopyOfDiscontinuedBookBecomesDiscontinued() {
        book.setPublicationStatus(PublicationStatus.DISCONTINUED);
        BookCopy copy = BookCopy.builder()
                .book(book)
                .barcode("BK-EJ-01-COPY-02")
                .status(CopyStatus.BORROWED)
                .build();
        when(bookCopyRepository.findByIdForUpdate(56L)).thenReturn(Optional.of(copy));

        bookCopyService.markReturned(56L);

        assertThat(copy.getStatus()).isEqualTo(CopyStatus.DISCONTINUED);
    }

    @Test
    @DisplayName("Discontinued books cannot be borrowed or reserved")
    void discontinuedBookCannotCirculate() {
        book.setPublicationStatus(PublicationStatus.DISCONTINUED);
        BookCopy copy = BookCopy.builder()
                .book(book)
                .barcode("BK-EJ-01-COPY-01")
                .status(CopyStatus.AVAILABLE)
                .build();
        when(bookCopyRepository.findByIdForUpdate(55L)).thenReturn(Optional.of(copy));

        assertThatThrownBy(() -> bookCopyService.markBorrowed(55L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("discontinued");
        assertThatThrownBy(() -> bookCopyService.markReserved(55L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("discontinued");

        assertThat(copy.getStatus()).isEqualTo(CopyStatus.AVAILABLE);
    }
}
