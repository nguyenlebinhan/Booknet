package grp3.java.booknet.entity.book;

import grp3.java.booknet.entity.BaseEntity;
import grp3.java.booknet.enums.book.CopyStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;

@Entity
@Table(name = "book_copies")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @SuperBuilder
@ToString(onlyExplicitlyIncluded = true)
public class BookCopy extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @ToString.Include
    @Column(nullable = false, unique = true, length = 40)
    private String barcode;

    @Column(name = "shelf_location", length = 50)
    private String shelfLocation;

    // CONDITION is a reserved word in MySQL, so the column has its own name; the field stays "condition".
    @Column(name = "book-condition", length = 255)
    private String condition;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private CopyStatus status = CopyStatus.AVAILABLE;

    @Column(name = "acquired_date")
    private LocalDate acquiredDate;
}
