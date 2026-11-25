package grp3.java.booknet.service.borrowing;

import java.util.List;
import java.util.Set;

public interface FavoriteBookService {

    boolean toggleFavorite(Long userId, Long bookId);

    void addFavorite(Long userId, Long bookId);

    void removeFavorite(Long userId, Long bookId);

    boolean isFavorite(Long userId, Long bookId);

    Set<Long> getFavoriteBookIds(Long userId);
}
