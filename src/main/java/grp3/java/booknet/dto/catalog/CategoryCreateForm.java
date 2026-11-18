package grp3.java.booknet.dto.catalog;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CategoryCreateForm {

    @NotBlank(message = "Category name is required.")
    @Size(max = 100, message = "Category name cannot exceed 100 characters.")
    private String name;

    private Integer parentId;
}
