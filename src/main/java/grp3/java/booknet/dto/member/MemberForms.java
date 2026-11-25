package grp3.java.booknet.dto.member;

import jakarta.validation.constraints.*;
import java.time.LocalDate;

public final class MemberForms {
    private MemberForms() { }
    public record AccessForm(@NotNull Boolean enabled, @NotNull @PositiveOrZero Long revision) { }
    public record StatusForm(@NotNull grp3.java.booknet.enums.user.UserStatus status,
                             @NotNull @PositiveOrZero Long revision) { }
    public record ActivationForm(
            @AssertTrue(message = "Confirm that identity documents were checked in person.") boolean identityChecked,
            @NotNull(message = "Enter the card expiry date.") @Future(message = "Expiry must be in the future.") LocalDate expiresOn) { }
}
