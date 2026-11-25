package grp3.java.booknet.dto.book.request;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
public class BookCopyForm {

    @Size(max = 40, message = "Barcode must not exceed 40 characters")
    private String barcode;

    @Size(max = 50, message = "Shelf location must not exceed 50 characters")
    private String shelfLocation;

    @Size(max = 255, message = "Condition must not exceed 255 characters")
    private String condition;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate acquiredDate;
}
