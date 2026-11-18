package grp3.java.booknet.repository.catalog;

import grp3.java.booknet.entity.book.Book;
import grp3.java.booknet.entity.book_service.Loan;
import grp3.java.booknet.enums.book.PublicationStatus;
import grp3.java.booknet.enums.book_service.LoanStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

/**
 * Read-only figures for the public catalog: popularity and return dates. It only reads
 * loans and books; circulation and book management keep ownership of those tables.
 */
public interface CatalogStatsRepository extends Repository<Loan, Long> {

    /** Earliest due date among copies of this book that are on loan now. */
    @Query("select min(l.dueDate) from Loan l where l.copy.book.id = :bookId and l.status in :statuses")
    LocalDate findEarliestDueDate(@Param("bookId") Long bookId, @Param("statuses") Collection<LoanStatus> statuses);

    /** Published books ranked by how often they have been borrowed, most borrowed first. */
    @Query("""
        select l.copy.book.id from Loan l
        where l.copy.book.deleted = false and l.copy.book.publicationStatus = :status
        group by l.copy.book.id
        order by count(l) desc, l.copy.book.id desc
        """)
    List<Long> findMostBorrowedBookIds(@Param("status") PublicationStatus status, Pageable pageable);

    /** How many times each of these books has been borrowed: rows of [bookId, count]. Books never borrowed are absent. */
    @Query("select l.copy.book.id, count(l) from Loan l where l.copy.book.id in :bookIds group by l.copy.book.id")
    List<Object[]> countLoansByBook(@Param("bookIds") Collection<Long> bookIds);

    /** Genres with the most published books: rows of [categoryId, name, titles], largest first. */
    @Query("""
        select c.id, c.name, count(distinct b.id) from Book b join b.categories c
        where b.deleted = false and b.publicationStatus = :status
        group by c.id, c.name
        order by count(distinct b.id) desc, c.name asc
        """)
    List<Object[]> findLargestGenres(@Param("status") PublicationStatus status, Pageable pageable);

    /** Other published books sharing a genre or an author with this one. */
    @Query("""
        select distinct b from Book b
        left join b.categories c left join b.authors a
        where b.id <> :bookId and b.deleted = false and b.publicationStatus = :status
          and (c.id in :categoryIds or a.id in :authorIds)
        order by b.id desc
        """)
    List<Book> findRelated(@Param("bookId") Long bookId, @Param("categoryIds") Collection<Integer> categoryIds,
                           @Param("authorIds") Collection<Integer> authorIds, @Param("status") PublicationStatus status,
                           Pageable pageable);
}
