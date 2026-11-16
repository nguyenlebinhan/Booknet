package grp3.java.booknet.service;

import grp3.java.booknet.dto.book.request.BookSearchFilter;
import grp3.java.booknet.dto.book.response.BookListRowDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface BookService {

    Page<BookListRowDto> getBookManagementList(BookSearchFilter filter, Pageable pageable);

    Long createBook(grp3.java.booknet.dto.book.request.BookCreateForm form);

    grp3.java.booknet.dto.book.request.BookEditForm getBookForEdit(Long id);

    void updateBook(Long id, grp3.java.booknet.dto.book.request.BookEditForm form);
}
