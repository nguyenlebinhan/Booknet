package grp3.java.booknet.service.book;

import grp3.java.booknet.entity.book.BookCopy;
import grp3.java.booknet.enums.book.CopyStatus;

/**
 * Public domain boundary for Borrowing/Reservation modules.
 * Those modules should call this service rather than changing BookCopy.status
 * directly or updating BookCopyRepository themselves.
 */
public interface BookInventoryService {

    BookCopy changeCopyStatus(Long copyId, CopyStatus targetStatus);

    BookCopy markBorrowed(Long copyId);
    BookCopy markReserved(Long copyId);
    BookCopy releaseReservation(Long copyId);
    BookCopy markReturned(Long copyId);
}
