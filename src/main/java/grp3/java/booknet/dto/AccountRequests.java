package grp3.java.booknet.dto;

import jakarta.validation.constraints.*;
import grp3.java.booknet.config.AccountPolicy;

public final class AccountRequests {
    private AccountRequests() { }

    public record EmailRequest(@NotBlank @Email @Size(max = 120) String email) { }

    public record ResetPassword(
            @NotBlank @Size(max = 200) String token,
            @NotBlank @Size(min = AccountPolicy.PASSWORD_MIN_LENGTH, max = AccountPolicy.PASSWORD_MAX_LENGTH)
            @Pattern(regexp = AccountPolicy.PASSWORD_PATTERN, message = AccountPolicy.PASSWORD_MESSAGE) String newPassword,
            @NotBlank String confirmPassword) {
        @AssertTrue(message = "Mật khẩu xác nhận không trùng khớp")
        public boolean isPasswordConfirmed() {
            return newPassword != null && newPassword.equals(confirmPassword);
        }
    }

    public record ChangePassword(
            @NotBlank @Size(max = 200) String currentPassword,
            @NotBlank @Size(min = AccountPolicy.PASSWORD_MIN_LENGTH, max = AccountPolicy.PASSWORD_MAX_LENGTH)
            @Pattern(regexp = AccountPolicy.PASSWORD_PATTERN, message = AccountPolicy.PASSWORD_MESSAGE) String newPassword,
            @NotBlank String confirmPassword) {
        @AssertTrue(message = "Mật khẩu xác nhận không trùng khớp")
        public boolean isPasswordConfirmed() {
            return newPassword != null && newPassword.equals(confirmPassword);
        }
    }

    public record ProfileUpdate(
            @NotBlank @Size(min = 2, max = 120) String fullName,
            @Size(max = 20) @Pattern(regexp = "^[+0-9() .-]*$") String phone,
            @Size(max = 255) String address,
            @Size(max = 500) @Pattern(regexp = "^(https?://[^\\s]+)?$") String avatarUrl) { }
}
