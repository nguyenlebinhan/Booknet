package grp3.java.booknet.service.book;

import grp3.java.booknet.dto.book.request.BookCopyForm;
import grp3.java.booknet.dto.book.response.InventoryStatsDto;
import grp3.java.booknet.entity.book.BookCopy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface BookCopyService {

    BookCopy createCopy(Long bookId, BookCopyForm form);
    BookCopy updateCopy(Long copyId, BookCopyForm form);
    BookCopy getCopy(Long copyId);
    Page<BookCopy> getCopies(Long bookId, Pageable pageable);
    List<BookCopy> getAvailableCopies(Long bookId);
    InventoryStatsDto getInventoryStats(Long bookId);
}
