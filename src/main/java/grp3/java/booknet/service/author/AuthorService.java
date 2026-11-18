package grp3.java.booknet.service.author;

import grp3.java.booknet.dto.author.AuthorCreateForm;
import grp3.java.booknet.entity.book.Author;
import grp3.java.booknet.entity.book.Book;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface AuthorService {

    Page<Author> listAuthors(String query, Pageable pageable);

    Page<Author> listAuthorsByInitial(String initial, String query, Pageable pageable);

    List<String> listAuthorInitials();

    Optional<Author> findAuthor(Integer authorId);

    Page<Book> searchBooksByAuthor(Integer authorId, String query, Pageable pageable);

    Author createAuthor(AuthorCreateForm form);
}
