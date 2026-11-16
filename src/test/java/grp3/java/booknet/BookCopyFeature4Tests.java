package grp3.java.booknet;

import grp3.java.booknet.dto.book.request.BookCopyForm;
import grp3.java.booknet.dto.book.response.InventoryStatsDto;
import grp3.java.booknet.entity.book.Book;
import grp3.java.booknet.entity.book.BookCopy;
import grp3.java.booknet.enums.book.CopyStatus;
import grp3.java.booknet.repository.book.BookCopyRepository;
import grp3.java.booknet.repository.book.BookRepository;
import grp3.java.booknet.service.book.BookCopyService;
import grp3.java.booknet.service.book.BookInventoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class BookCopyFeature4Tests {

    @Autowired private BookCopyService bookCopyService;
    @Autowired private BookInventoryService inventoryService;
    @Autowired private BookCopyRepository copyRepository;
    @Autowired private BookRepository bookRepository;

    private Book book;

    @BeforeEach
    void setUp() {
        copyRepository.deleteAll();
        bookRepository.deleteAll();
        book = bookRepository.save(Book.builder()
                .bookCode("BK-F4-TEST")
                .title("Feature 4 Test Book")
                .isbn("9780000000001")
                .build());
    }

    @Test
    @DisplayName("Creates copy with normalized barcode and AVAILABLE status")
    void createsCopy() {
        BookCopyForm form = form("  bc-001  ");
        BookCopy copy = bookCopyService.createCopy(book.getId(), form);

        assertThat(copy.getBarcode()).isEqualTo("BC-001");
        assertThat(copy.getStatus()).isEqualTo(CopyStatus.AVAILABLE);
        assertThat(copy.getBook().getId()).isEqualTo(book.getId());
    }

    @Test
    @DisplayName("Generates barcode when user leaves it blank")
    void generatesBarcode() {
        BookCopy copy = bookCopyService.createCopy(book.getId(), form(" "));
        assertThat(copy.getBarcode()).isNotBlank().hasSizeLessThanOrEqualTo(40);
        assertThat(copyRepository.existsByBarcode(copy.getBarcode())).isTrue();
    }

    @Test
    @DisplayName("Rejects duplicate barcode")
    void rejectsDuplicateBarcode() {
        bookCopyService.createCopy(book.getId(), form("DUP-001"));

        assertThatThrownBy(() -> bookCopyService.createCopy(book.getId(), form("dup-001")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    @DisplayName("Updates copy metadata without changing its status")
    void updatesMetadataWithoutChangingStatus() {
        BookCopy copy = bookCopyService.createCopy(book.getId(), form("UP-001"));
        inventoryService.markBorrowed(copy.getId());

        BookCopyForm update = form("UP-001");
        update.setShelfLocation("A-01-02");
        update.setCondition("Good");

        BookCopy saved = bookCopyService.updateCopy(copy.getId(), update);

        assertThat(saved.getShelfLocation()).isEqualTo("A-01-02");
        assertThat(saved.getCondition()).isEqualTo("Good");
        assertThat(saved.getStatus()).isEqualTo(CopyStatus.BORROWED);
    }

    @Test
    @DisplayName("Allows available to borrowed and borrowed to available")
    void changesBorrowStatus() {
        BookCopy copy = bookCopyService.createCopy(book.getId(), form("FLOW-001"));

        assertThat(inventoryService.markBorrowed(copy.getId()).getStatus())
                .isEqualTo(CopyStatus.BORROWED);
        assertThat(inventoryService.markReturned(copy.getId()).getStatus())
                .isEqualTo(CopyStatus.AVAILABLE);
    }

    @Test
    @DisplayName("Rejects invalid status transition")
    void rejectsInvalidTransition() {
        BookCopy copy = bookCopyService.createCopy(book.getId(), form("FLOW-002"));

        inventoryService.markBorrowed(copy.getId());

        assertThatThrownBy(() -> inventoryService.changeCopyStatus(copy.getId(), CopyStatus.RESERVED))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("Inventory statistics include every defined status")
    void calculatesInventoryStats() {
        BookCopy available = bookCopyService.createCopy(book.getId(), form("ST-001"));
        BookCopy borrowed = bookCopyService.createCopy(book.getId(), form("ST-002"));
        BookCopy reserved = bookCopyService.createCopy(book.getId(), form("ST-003"));
        BookCopy damaged = bookCopyService.createCopy(book.getId(), form("ST-004"));

        inventoryService.markBorrowed(borrowed.getId());
        inventoryService.markReserved(reserved.getId());
        inventoryService.changeCopyStatus(damaged.getId(), CopyStatus.DAMAGED);

        InventoryStatsDto stats = bookCopyService.getInventoryStats(book.getId());

        assertThat(stats.totalCopies()).isEqualTo(4);
        assertThat(stats.availableCopies()).isEqualTo(1);
        assertThat(stats.borrowedCopies()).isEqualTo(1);
        assertThat(stats.reservedCopies()).isEqualTo(1);
        assertThat(stats.damagedCopies()).isEqualTo(1);
        assertThat(stats.lostCopies()).isZero();
        assertThat(stats.discontinuedCopies()).isZero();
    }

    private BookCopyForm form(String barcode) {
        BookCopyForm form = new BookCopyForm();
        form.setBarcode(barcode);
        form.setShelfLocation("A-01");
        form.setCondition("New");
        return form;
    }
}
