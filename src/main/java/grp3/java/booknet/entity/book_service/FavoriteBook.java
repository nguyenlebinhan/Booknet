package grp3.java.booknet.entity.book_service;

import grp3.java.booknet.entity.BaseEntity;
import grp3.java.booknet.entity.book.Book;
import grp3.java.booknet.entity.user.User;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Entity
@Table(name = "favorite_books",
        uniqueConstraints = @UniqueConstraint(name = "uk_user_book_favorite", columnNames = {"user_id", "book_id"}),
        indexes = {
                @Index(name = "idx_favorite_user", columnList = "user_id"),
                @Index(name = "idx_favorite_book", columnList = "book_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@ToString(onlyExplicitlyIncluded = true)
public class FavoriteBook extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @ToString.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @Column(name = "saved_at", nullable = false)
    private LocalDateTime savedAt;

    @PrePersist
    protected void onSave() {
        if (this.savedAt == null) {
            this.savedAt = LocalDateTime.now();
        }
    }
}
