package grp3.java.booknet.dto;

import grp3.java.booknet.config.AccountPolicy;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PasswordResetForm {
    @NotBlank @Size(max = 200)
    private String token;

    @NotBlank(message = AccountPolicy.PASSWORD_MESSAGE)
    @Size(min = AccountPolicy.PASSWORD_MIN_LENGTH, max = AccountPolicy.PASSWORD_MAX_LENGTH, message = AccountPolicy.PASSWORD_MESSAGE)
    @Pattern(regexp = AccountPolicy.PASSWORD_PATTERN, message = AccountPolicy.PASSWORD_MESSAGE)
    private String password;

    @NotBlank(message = "Vui lòng xác nhận mật khẩu")
    private String confirmPassword;
}
