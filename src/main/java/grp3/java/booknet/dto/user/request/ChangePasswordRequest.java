package grp3.java.booknet.dto.user.request;

import grp3.java.booknet.config.account.AccountPolicy;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @NotBlank(message = "Please enter your current password.")
        @Size(max = 200, message = "Current password must not exceed 200 characters.") String currentPassword,
        @NotBlank(message = AccountPolicy.PASSWORD_MESSAGE)
        @Size(min = AccountPolicy.PASSWORD_MIN_LENGTH, max = AccountPolicy.PASSWORD_MAX_LENGTH, message = AccountPolicy.PASSWORD_MESSAGE)
        @Pattern(regexp = AccountPolicy.PASSWORD_PATTERN, message = AccountPolicy.PASSWORD_MESSAGE) String newPassword,
        @NotBlank(message = "Please confirm your password.") String confirmPassword) {
    @AssertTrue(message = "Passwords do not match.")
    public boolean isPasswordConfirmed() {
        return newPassword != null && newPassword.equals(confirmPassword);
    }
}
