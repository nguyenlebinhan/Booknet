package grp3.java.booknet.service;

import grp3.java.booknet.dto.BookItemDto;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class BookCatalogService {

    public List<BookItemDto> getFeaturedBooks() {
        List<BookItemDto> books = new ArrayList<>();

        books.add(BookItemDto.builder()
                .id(1L)
                .title("Clean Code: A Handbook of Agile Software Craftsmanship")
                .author("Robert C. Martin")
                .category("Công Nghệ Thông Tin")
                .isbn("978-0132350884")
                .availableCopies(7)
                .totalCopies(10)
                .rating(4.9)
                .coverImage("https://images.unsplash.com/photo-1532012164546-f432f2e3edd4?w=500&auto=format&fit=crop&q=80")
                .description("Cẩm nang kinh điển về nghệ thuật viết mã sạch, kiến trúc phần mềm và kỹ thuật tái cấu trúc code chuyên nghiệp.")
                .build());

        books.add(BookItemDto.builder()
                .id(2L)
                .title("Spring Boot in Action")
                .author("Craig Walls")
                .category("Công Nghệ Thông Tin")
                .isbn("978-1617292545")
                .availableCopies(4)
                .totalCopies(6)
                .rating(4.8)
                .coverImage("https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?w=500&auto=format&fit=crop&q=80")
                .description("Hướng dẫn phát triển ứng dụng Java doanh nghiệp hiện đại với hệ sinh thái Spring Boot mạnh mẽ và hiệu năng cao.")
                .build());

        books.add(BookItemDto.builder()
                .id(3L)
                .title("Giải Thuật và Lập Trình Nâng Cao")
                .author("Lê Minh Hoàng")
                .category("Giáo Trình Đại Học")
                .isbn("978-6048039211")
                .availableCopies(12)
                .totalCopies(15)
                .rating(4.7)
                .coverImage("https://images.unsplash.com/photo-1512820790803-83ca734da794?w=500&auto=format&fit=crop&q=80")
                .description("Tài liệu nền tảng dành cho sinh viên ngành công nghệ, đào sâu cấu trúc dữ liệu, quy hoạch động và đồ thị.")
                .build());

        books.add(BookItemDto.builder()
                .id(4L)
                .title("Tư Duy Nhanh Và Chậm (Thinking, Fast and Slow)")
                .author("Daniel Kahneman")
                .category("Khoa Học Tâm Lý")
                .isbn("978-0374533557")
                .availableCopies(5)
                .totalCopies(8)
                .rating(4.9)
                .coverImage("https://images.unsplash.com/photo-1497633762265-9d179a990aa6?w=500&auto=format&fit=crop&q=80")
                .description("Kiệt tác đoạt giải Nobel về hai hệ thống điều khiển suy nghĩ của con người: trực giác cảm xúc và lý trí phân tích.")
                .build());

        books.add(BookItemDto.builder()
                .id(5L)
                .title("Đắc Nhân Tâm (How to Win Friends and Influence People)")
                .author("Dale Carnegie")
                .category("Kỹ Năng Sống")
                .isbn("978-1439167342")
                .availableCopies(9)
                .totalCopies(10)
                .rating(4.8)
                .coverImage("https://images.unsplash.com/photo-1476275466078-4007374efbbe?w=500&auto=format&fit=crop&q=80")
                .description("Cuốn sách gối đầu giường nổi tiếng toàn cầu về nghệ thuật giao tiếp, ứng xử và thu phục lòng người.")
                .build());

        books.add(BookItemDto.builder()
                .id(6L)
                .title("Sapiens: Lược Sử Loài Người")
                .author("Yuval Noah Harari")
                .category("Lịch Sử & Triết Học")
                .isbn("978-0062316097")
                .availableCopies(6)
                .totalCopies(8)
                .rating(4.9)
                .coverImage("https://images.unsplash.com/photo-1495446815901-a7297e633e8d?w=500&auto=format&fit=crop&q=80")
                .description("Hành trình khám phá quá trình tiến hóa và kiến tạo nền văn minh nhân loại từ thời kỳ đồ đá đến kỷ nguyên trí tuệ nhân tạo.")
                .build());

        return books;
    }
}
