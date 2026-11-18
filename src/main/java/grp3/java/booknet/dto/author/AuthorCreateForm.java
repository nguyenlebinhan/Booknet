package grp3.java.booknet.dto.author;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
public class AuthorCreateForm {

    @NotBlank(message = "Author name is required.")
    @Size(max = 150, message = "Author name cannot exceed 150 characters.")
    private String name;

    @Size(max = 10000, message = "Biography cannot exceed 10,000 characters.")
    private String bio;

    private MultipartFile image;
}
