package grp3.java.booknet.repository.book_service;

import grp3.java.booknet.entity.book_service.FavoriteBook;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface FavoriteBookRepository extends JpaRepository<FavoriteBook, Long> {

    List<FavoriteBook> findByUser_IdOrderBySavedAtDesc(Long userId);

    boolean existsByUser_IdAndBook_Id(Long userId, Long bookId);

    Optional<FavoriteBook> findByUser_IdAndBook_Id(Long userId, Long bookId);

    void deleteByUser_IdAndBook_Id(Long userId, Long bookId);

    long countByUser_Id(Long userId);
}
