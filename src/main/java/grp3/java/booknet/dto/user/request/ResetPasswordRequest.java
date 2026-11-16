package grp3.java.booknet.dto.user.request;

import grp3.java.booknet.config.AccountPolicy;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        @NotBlank(message = "A password reset token is required.")
        @Size(max = 200, message = "This password reset token is invalid.") String token,
        @NotBlank(message = AccountPolicy.PASSWORD_MESSAGE)
        @Size(min = AccountPolicy.PASSWORD_MIN_LENGTH, max = AccountPolicy.PASSWORD_MAX_LENGTH, message = AccountPolicy.PASSWORD_MESSAGE)
        @Pattern(regexp = AccountPolicy.PASSWORD_PATTERN, message = AccountPolicy.PASSWORD_MESSAGE) String newPassword,
        @NotBlank(message = "Please confirm your password.") String confirmPassword) {
    @AssertTrue(message = "Passwords do not match.")
    public boolean isPasswordConfirmed() {
        return newPassword != null && newPassword.equals(confirmPassword);
    }
}
