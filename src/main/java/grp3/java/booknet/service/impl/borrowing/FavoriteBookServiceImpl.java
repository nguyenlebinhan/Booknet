package grp3.java.booknet.service.impl.borrowing;

import grp3.java.booknet.entity.book.Book;
import grp3.java.booknet.entity.book_service.FavoriteBook;
import grp3.java.booknet.entity.user.User;
import grp3.java.booknet.exception.account.AccountException;
import grp3.java.booknet.repository.book.BookRepository;
import grp3.java.booknet.repository.book_service.FavoriteBookRepository;
import grp3.java.booknet.repository.user.UserRepository;
import grp3.java.booknet.service.borrowing.FavoriteBookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FavoriteBookServiceImpl implements FavoriteBookService {

    private final FavoriteBookRepository favoriteBookRepository;
    private final UserRepository userRepository;
    private final BookRepository bookRepository;

    @Override
    @Transactional
    public boolean toggleFavorite(Long userId, Long bookId) {
        if (favoriteBookRepository.existsByUser_IdAndBook_Id(userId, bookId)) {
            favoriteBookRepository.deleteByUser_IdAndBook_Id(userId, bookId);
            log.info("Removed book {} from user {} favorites", bookId, userId);
            return false;
        } else {
            addFavorite(userId, bookId);
            return true;
        }
    }

    @Override
    @Transactional
    public void addFavorite(Long userId, Long bookId) {
        if (favoriteBookRepository.existsByUser_IdAndBook_Id(userId, bookId)) {
            return;
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AccountException(HttpStatus.NOT_FOUND, "User not found"));
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new AccountException(HttpStatus.NOT_FOUND, "Book not found"));

        FavoriteBook favorite = FavoriteBook.builder()
                .user(user)
                .book(book)
                .savedAt(LocalDateTime.now())
                .build();
        favoriteBookRepository.save(favorite);
        log.info("Saved book {} to user {} favorites", bookId, userId);
    }

    @Override
    @Transactional
    public void removeFavorite(Long userId, Long bookId) {
        favoriteBookRepository.deleteByUser_IdAndBook_Id(userId, bookId);
        log.info("Removed book {} from user {} favorites", bookId, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isFavorite(Long userId, Long bookId) {
        if (userId == null) return false;
        return favoriteBookRepository.existsByUser_IdAndBook_Id(userId, bookId);
    }

    @Override
    @Transactional(readOnly = true)
    public Set<Long> getFavoriteBookIds(Long userId) {
        if (userId == null) return Collections.emptySet();
        return favoriteBookRepository.findByUser_IdOrderBySavedAtDesc(userId).stream()
                .map(f -> f.getBook().getId())
                .collect(Collectors.toSet());
    }
}
