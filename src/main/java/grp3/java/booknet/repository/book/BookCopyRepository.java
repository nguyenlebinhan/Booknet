package grp3.java.booknet.repository.book;

import grp3.java.booknet.entity.book.BookCopy;
import grp3.java.booknet.enums.book.CopyStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookCopyRepository extends JpaRepository<BookCopy, Long> {

    List<BookCopy> findByBook_Id(Long bookId);

    Optional<BookCopy> findFirstByBook_IdAndStatus(Long bookId, CopyStatus status);
}

