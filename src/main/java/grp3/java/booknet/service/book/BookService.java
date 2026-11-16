package grp3.java.booknet.service.book;

public interface BookService {

    Long createBook(grp3.java.booknet.dto.book.request.BookCreateForm form);

    grp3.java.booknet.dto.book.request.BookEditForm getBookForEdit(Long id);

    void updateBook(Long id, grp3.java.booknet.dto.book.request.BookEditForm form);

    void updateAndPublishBook(Long id, grp3.java.booknet.dto.book.request.BookEditForm form);
}
