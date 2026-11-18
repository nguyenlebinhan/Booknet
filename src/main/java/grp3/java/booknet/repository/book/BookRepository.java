package grp3.java.booknet.repository.book;

import grp3.java.booknet.entity.book.Book;
import grp3.java.booknet.enums.book.PublicationStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookRepository extends JpaRepository<Book, Long>, JpaSpecificationExecutor<Book> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from Book b where b.id = :id")
    Optional<Book> findByIdForUpdate(@Param("id") Long id);

    Optional<Book> findByBookCode(String bookCode);

    Optional<Book> findByIsbn(String isbn);

    boolean existsByBookCode(String bookCode);

    boolean existsByIsbn(String isbn);

    boolean existsByBookCodeAndIdNot(String bookCode, Long id);

    boolean existsByIsbnAndIdNot(String isbn, Long id);

    @Query(value = """
            select distinct b from Book b join b.authors author
            where author.id = :authorId and b.deleted = false
              and (:query = '' or lower(b.title) like lower(concat('%', :query, '%'))
                   or lower(b.isbn) like lower(concat('%', :query, '%'))
                   or lower(b.bookCode) like lower(concat('%', :query, '%')))
            """,
            countQuery = """
            select count(distinct b.id) from Book b join b.authors author
            where author.id = :authorId and b.deleted = false
              and (:query = '' or lower(b.title) like lower(concat('%', :query, '%'))
                   or lower(b.isbn) like lower(concat('%', :query, '%'))
                   or lower(b.bookCode) like lower(concat('%', :query, '%')))
            """)
    Page<Book> searchByAuthor(@Param("authorId") Integer authorId,
                              @Param("query") String query,
                              Pageable pageable);

    long countByDeletedFalseAndPublicationStatus(PublicationStatus publicationStatus);

    @Query("select distinct b.publishYear from Book b where b.deleted = false and b.publicationStatus = :status and b.publishYear is not null order by b.publishYear desc")
    List<Short> findDistinctPublishedYears(@Param("status") PublicationStatus status);

    @Query("select distinct b.publishYear from Book b where b.deleted = false and b.publishYear is not null order by b.publishYear desc")
    List<Short> findDistinctPublishYears();
}
