package grp3.java.booknet.service.book;

import grp3.java.booknet.dto.book.request.BookCreateForm;
import grp3.java.booknet.dto.book.request.BookEditForm;

public interface BookService {

    Long createBook(BookCreateForm form);

    BookEditForm getBookForEdit(Long id);

    void updateBook(Long id, BookEditForm form);

    void updateAndPublishBook(Long id, BookEditForm form);
}
