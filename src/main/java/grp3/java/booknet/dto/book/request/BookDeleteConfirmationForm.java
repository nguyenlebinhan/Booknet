package grp3.java.booknet.dto.book.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BookDeleteConfirmationForm {
    @NotBlank(message = "Please enter the book code to confirm.")
    @Size(max = 50, message = "Confirmation code must not exceed 50 characters.")
    private String confirmationCode;
}
