package grp3.java.booknet.service;

import grp3.java.booknet.dto.book.response.BookDeletionCheckDto;

public interface BookLifecycleService {
    BookDeletionCheckDto checkHardDelete(Long bookId);
    void hardDelete(Long bookId, String confirmationCode);
    void discontinue(Long bookId);
}
