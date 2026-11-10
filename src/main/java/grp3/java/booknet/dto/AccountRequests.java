package grp3.java.booknet.dto;

import jakarta.validation.constraints.*;
import grp3.java.booknet.config.AccountPolicy;

public final class AccountRequests {
    private AccountRequests() { }

    public record EmailRequest(
            @NotBlank(message = "Please enter your email address.")
            @Email(message = "Please enter a valid email address.")
            @Size(max = 120, message = "Email address must not exceed 120 characters.") String email) { }

    public record ResetPassword(
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

    public record ChangePassword(
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

    public record ProfileUpdate(
            @NotBlank(message = "Please enter your full name.")
            @Size(min = 2, max = 120, message = "Full name must be between 2 and 120 characters.") String fullName,
            @Size(max = 20, message = "Phone number must not exceed 20 characters.")
            @Pattern(regexp = "^[+0-9() .-]*$", message = "Please enter a valid phone number.") String phone,
            @Size(max = 255, message = "Address must not exceed 255 characters.") String address,
            @Size(max = 500, message = "Avatar URL must not exceed 500 characters.")
            @Pattern(regexp = "^(https?://[^\\s]+)?$", message = "Avatar URL must start with http:// or https:// and contain no spaces.") String avatarUrl) { }
}
