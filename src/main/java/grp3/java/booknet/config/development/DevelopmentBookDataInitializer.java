package grp3.java.booknet.config.development;

import java.util.Set;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import grp3.java.booknet.entity.book.Author;
import grp3.java.booknet.entity.book.Book;
import grp3.java.booknet.entity.book.BookCopy;
import grp3.java.booknet.entity.book.Category;
import grp3.java.booknet.entity.book.Publisher;
import grp3.java.booknet.enums.book.CopyStatus;
import grp3.java.booknet.enums.book.PublicationStatus;
import grp3.java.booknet.repository.book.AuthorRepository;
import grp3.java.booknet.repository.book.BookCopyRepository;
import grp3.java.booknet.repository.book.BookRepository;
import grp3.java.booknet.repository.book.CategoryRepository;
import grp3.java.booknet.repository.book.PublisherRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Seeds demo books and their physical copies for local development / testing.
 * Runs only on the "local" Spring profile and skips if books already exist.
 */
@Slf4j
@Component
@Profile("local")
@Order(2)   // runs after DevelopmentUserDataInitializer (order 1)
@RequiredArgsConstructor
public class DevelopmentBookDataInitializer implements CommandLineRunner {

    private final AuthorRepository      authorRepo;
    private final PublisherRepository   publisherRepo;
    private final CategoryRepository    categoryRepo;
    private final BookRepository        bookRepo;
    private final BookCopyRepository    copyRepo;

    @Override
    @Transactional
    public void run(String... args) {
        if (bookRepo.count() > 0) {
            log.info("Book seed data already present – skipping DevelopmentBookDataInitializer.");
            return;
        }

        log.info("Seeding demo books and copies…");

        // ── Authors ──────────────────────────────────────────────────────────
        Author nguyenNhatAnh = author("Nguyễn Nhật Ánh");
        Author toHoai        = author("Tô Hoài");
        Author haruki        = author("Haruki Murakami");
        Author georgeOrwell  = author("George Orwell");
        Author namCao        = author("Nam Cao");
        Author vuTrongPhung  = author("Vũ Trọng Phụng");
        Author tknh          = author("Nguyễn Du");
        Author huyenChip     = author("Huyền Chip");
        Author fpGarcia      = author("F. Scott Fitzgerald");
        Author dostoevsky    = author("Fyodor Dostoevsky");

        // ── Publishers ───────────────────────────────────────────────────────
        Publisher nxbTrePublisher  = publisher("NXB Trẻ");
        Publisher nxbKimDong       = publisher("NXB Kim Đồng");
        Publisher nxbHoiNhaVan     = publisher("NXB Hội Nhà Văn");
        Publisher penguin          = publisher("Penguin Books");
        Publisher scribner         = publisher("Scribner");

        // ── Categories ───────────────────────────────────────────────────────
        Category vanHocVN   = category("Văn học Việt Nam");
        Category vanHocNN   = category("Văn học nước ngoài");
        Category thieu_nhi  = category("Thiếu nhi");
        Category tieu_thuyet= category("Tiểu thuyết");
        Category co_dien    = category("Văn học cổ điển");
        Category du_ki      = category("Du ký - Hồi ký");

        // ── Books + copies ───────────────────────────────────────────────────
        seedBook("Tôi Thấy Hoa Vàng Trên Cỏ Xanh",
                "978-604-1-00001-1", nguyenNhatAnh, nxbTrePublisher, (short) 2010,
                Set.of(vanHocVN, tieu_thuyet),
                "Câu chuyện tuổi thơ đẹp đẽ và đầy xúc động ở làng quê miền Trung Việt Nam.",
                "/images/home/cover-hoa-vang.jpg",
                "CP-001", "Floor 1 - Shelf A", 5);

        seedBook("Mắt Biếc",
                "978-604-1-00002-8", nguyenNhatAnh, nxbTrePublisher, (short) 1990,
                Set.of(vanHocVN, tieu_thuyet),
                "Chuyện tình buồn của Ngạn và Hà Lan – một trong những cuốn sách hay nhất của Nguyễn Nhật Ánh.",
                "/images/home/cover-hoa-vang.jpg",
                "CP-002", "Floor 1 - Shelf B", 5);

        seedBook("Dế Mèn Phiêu Lưu Ký",
                "978-604-1-00003-5", toHoai, nxbKimDong, (short) 1941,
                Set.of(vanHocVN, thieu_nhi),
                "Những cuộc phiêu lưu kỳ thú của chú dế mèn dũng cảm – kinh điển văn học thiếu nhi Việt Nam.",
                "/images/home/cover-hoa-vang.jpg",
                "CP-003", "Floor 2 - Shelf C", 5);

        seedBook("Rừng Na Uy",
                "978-0-099-44882-2", haruki, nxbHoiNhaVan, (short) 2006,
                Set.of(vanHocNN, tieu_thuyet),
                "Tiểu thuyết tâm lý đậm chất thơ của Haruki Murakami về tình yêu, mất mát và trưởng thành.",
                "/images/home/cover-hoa-vang.jpg",
                "CP-004", "Floor 2 - Shelf D", 5);

        seedBook("1984",
                "978-0-451-52493-5", georgeOrwell, penguin, (short) 1949,
                Set.of(vanHocNN, tieu_thuyet),
                "Tác phẩm dystopia kinh điển về nhà nước toàn trị và cuộc đấu tranh cá nhân chống lại sự kiểm soát.",
                "/images/home/cover-hoa-vang.jpg",
                "CP-005", "Floor 3 - Shelf E", 5);

        seedBook("Chí Phèo",
                "978-604-1-00006-6", namCao, nxbHoiNhaVan, (short) 1941,
                Set.of(vanHocVN, co_dien),
                "Truyện ngắn hiện thực phê phán nổi tiếng nhất của Nam Cao về người nông dân bị tha hoá.",
                "/images/home/cover-hoa-vang.jpg",
                "CP-006", "Floor 1 - Shelf C", 5);

        seedBook("Số Đỏ",
                "978-604-1-00007-3", vuTrongPhung, nxbHoiNhaVan, (short) 1936,
                Set.of(vanHocVN, co_dien),
                "Tiểu thuyết châm biếm sâu sắc về xã hội Hà Nội những năm 1930.",
                "/images/home/cover-hoa-vang.jpg",
                "CP-007", "Floor 1 - Shelf D", 5);

        seedBook("Truyện Kiều",
                "978-604-1-00008-0", tknh, nxbHoiNhaVan, (short) 1820,
                Set.of(vanHocVN, co_dien),
                "Kiệt tác văn học chữ Nôm của Nguyễn Du – bản hùng ca về thân phận người phụ nữ trong xã hội phong kiến.",
                "/images/home/cover-hoa-vang.jpg",
                "CP-008", "Floor 3 - Shelf A", 5);

        seedBook("Xách Ba Lô Lên Và Đi",
                "978-604-1-00009-7", huyenChip, nxbTrePublisher, (short) 2013,
                Set.of(du_ki),
                "Hành trình xuyên Á một mình của cô gái Việt trẻ truyền cảm hứng cho hàng triệu độc giả.",
                "/images/home/cover-hoa-vang.jpg",
                "CP-009", "Floor 2 - Shelf A", 5);

        seedBook("Đại Gia Gatsby",
                "978-0-7432-7356-5", fpGarcia, scribner, (short) 1925,
                Set.of(vanHocNN, tieu_thuyet),
                "Kiệt tác của F. Scott Fitzgerald về giấc mơ Mỹ, tình yêu và bi kịch trong thời Prohibition.",
                "/images/home/cover-hoa-vang.jpg",
                "CP-010", "Floor 3 - Shelf B", 5);

        seedBook("Anh Em Nhà Karamazov",
                "978-0-374-52837-7", dostoevsky, penguin, (short) 1880,
                Set.of(vanHocNN, tieu_thuyet),
                "Kiệt tác cuối đời của Dostoevsky – một cuộc điều tra triết học sâu sắc về niềm tin, đạo đức và gia đình.",
                "/images/home/cover-hoa-vang.jpg",
                "CP-011", "Floor 3 - Shelf C", 5);

        log.info("Book seed data complete: {} books seeded.", bookRepo.count());
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private Author author(String name) {
        return authorRepo.findByName(name)
                .orElseGet(() -> authorRepo.save(Author.builder().name(name).build()));
    }

    private Publisher publisher(String name) {
        return publisherRepo.findByName(name)
                .orElseGet(() -> publisherRepo.save(Publisher.builder().name(name).build()));
    }

    private Category category(String name) {
        return categoryRepo.findByNameIgnoreCase(name)
                .orElseGet(() -> categoryRepo.save(Category.builder().name(name).build()));
    }

    private void seedBook(String title, String isbn, Author author, Publisher publisher,
                          short year, Set<Category> categories, String description,
                          String coverUrl, String barcodePrefix, String shelf, int copies) {

        if (bookRepo.findByIsbn(isbn).isPresent()) {
            log.info("  Book '{}' already exists – skipping.", title);
            return;
        }

        Book book = Book.builder()
                .title(title)
                .isbn(isbn)
                .authors(Set.of(author))
                .publisher(publisher)
                .publishYear(year)
                .categories(categories)
                .description(description)
                .coverUrl(coverUrl)
                .publicationStatus(PublicationStatus.PUBLISHED)
                .language("vi")
                .build();

        book = bookRepo.save(book);

        for (int i = 1; i <= copies; i++) {
            String barcode = barcodePrefix + "-" + String.format("%02d", i);
            if (!copyRepo.existsByBarcode(barcode)) {
                copyRepo.save(BookCopy.builder()
                        .book(book)
                        .barcode(barcode)
                        .shelfLocation(shelf)
                        .status(CopyStatus.AVAILABLE)
                        .build());
            }
        }

        log.info("  Seeded '{}' with {} copies ({}-01 … -{}).", title, copies,
                barcodePrefix, String.format("%02d", copies));
    }
}
