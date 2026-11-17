package grp3.java.booknet.dto.book.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookCreateForm {

    @NotBlank(message = "Title is required")
    @Size(max = 255, message = "Title cannot exceed 255 characters")
    private String title;

    @NotBlank(message = "Book code is required")
    @Size(max = 50, message = "Book code cannot exceed 50 characters")
    @Pattern(regexp = "^[A-Za-z0-9_-]+$", message = "Book code can only contain letters, numbers, hyphens and underscores")
    private String bookCode;

    @NotBlank(message = "ISBN is required")
    @Pattern(regexp = "^[0-9X-]{10,17}$", message = "Please enter a valid ISBN format")
    private String isbn;

    @NotEmpty(message = "At least one author must be selected")
    @Builder.Default
    private List<Long> authorIds = new ArrayList<>();

    @NotEmpty(message = "At least one category must be selected")
    @Builder.Default
    private List<Long> categoryIds = new ArrayList<>();

    private Long publisherId;

    @Size(max = 150, message = "Translator name cannot exceed 150 characters")
    private String translator;

    @Size(max = 50, message = "Edition cannot exceed 50 characters")
    private String edition;

    @NotBlank(message = "Language is required")
    @Size(max = 30, message = "Language cannot exceed 30 characters")
    @Builder.Default
    private String language = "vi";

    @NotNull(message = "Publication year is required")
    @Min(value = 1000, message = "Publication year must be valid (at least 1000)")
    @Max(value = 2100, message = "Publication year cannot be in the far future")
    private Short publishYear;

    @NotNull(message = "Page count is required")
    @Min(value = 1, message = "Page count must be at least 1")
    @Max(value = 20000, message = "Page count is unrealistically large")
    private Integer pageCount;

    @Size(max = 5000, message = "Description cannot exceed 5000 characters")
    private String description;

    @Size(max = 255, message = "Keywords cannot exceed 255 characters")
    private String keywords;

    @Size(max = 500, message = "Cover URL cannot exceed 500 characters")
    private String coverUrl;
}
