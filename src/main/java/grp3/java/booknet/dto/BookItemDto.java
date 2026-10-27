package grp3.java.booknet.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookItemDto {
    private Long id;
    private String title;
    private String author;
    private String category;
    private String isbn;
    private int availableCopies;
    private int totalCopies;
    private double rating;
    private String coverImage;
    private String description;
}
