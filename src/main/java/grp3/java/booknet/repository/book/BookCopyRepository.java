package grp3.java.booknet.repository.book;

import grp3.java.booknet.entity.book.BookCopy;
import grp3.java.booknet.enums.book.CopyStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookCopyRepository extends JpaRepository<BookCopy, Long> {

    @EntityGraph(attributePaths = "book")
    Optional<BookCopy> findByBarcode(String barcode);
    boolean existsByBarcode(String barcode);
    boolean existsByBarcodeAndIdNot(String barcode, Long id);

    @EntityGraph(attributePaths = "book")
    List<BookCopy> findByBook_Id(Long bookId);

    @EntityGraph(attributePaths = "book")
    Page<BookCopy> findByBook_Id(Long bookId, Pageable pageable);
    List<BookCopy> findByBook_IdAndStatus(Long bookId, CopyStatus status);
    List<BookCopy> findByStatus(CopyStatus status);

    long countByBook_Id(Long bookId);
    long countByBook_IdAndStatus(Long bookId, CopyStatus status);

    @EntityGraph(attributePaths = "book")
    @Query("select bc from BookCopy bc where bc.id = :id")
    Optional<BookCopy> findByIdWithBook(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = "book")
    @Query("select bc from BookCopy bc where bc.id = :id")
    Optional<BookCopy> findByIdForUpdate(@Param("id") Long id);

    @Query("SELECT bc.book.id, COUNT(bc) FROM BookCopy bc WHERE bc.book.id IN :bookIds GROUP BY bc.book.id")
    List<Object[]> countTotalCopiesByBookIds(@Param("bookIds") Collection<Long> bookIds);

    @Query("SELECT bc.book.id, COUNT(bc) FROM BookCopy bc WHERE bc.book.id IN :bookIds AND bc.status = :status GROUP BY bc.book.id")
    List<Object[]> countCopiesByBookIdsAndStatus(@Param("bookIds") Collection<Long> bookIds, @Param("status") CopyStatus status);

    @Query("select count(bc) from BookCopy bc where bc.book.id = :bookId and bc.status = :status")
    long countCopies(@Param("bookId") Long bookId, @Param("status") CopyStatus status);
}
