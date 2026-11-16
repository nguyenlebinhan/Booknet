package grp3.java.booknet.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotEmpty;
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
public class BorrowRequestForm {

    @NotEmpty(message = "Please select at least one book to borrow.")
    @Builder.Default
    private List<Long> bookIds = new ArrayList<>();

    @AssertTrue(message = "You must agree to BookNet Library's borrowing policy before confirming.")
    @Builder.Default
    private boolean termsAccepted = false;
}
