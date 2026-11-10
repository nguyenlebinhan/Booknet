package grp3.java.booknet.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;
import grp3.java.booknet.config.AccountPolicy;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserRegisterDto {

    @NotBlank(message = "Please enter your full name.")
    @Size(min = 2, max = 120, message = "Full name must be between 2 and 120 characters.")
    private String fullName;

    @NotBlank(message = "Please enter your email address.")
    @Email(message = "Please enter a valid email address.")
    @Size(max = 120, message = "Email address must not exceed 120 characters.")
    private String email;

    @NotBlank(message = "Please enter your password.")
    @Size(min = AccountPolicy.PASSWORD_MIN_LENGTH, max = AccountPolicy.PASSWORD_MAX_LENGTH, message = AccountPolicy.PASSWORD_MESSAGE)
    @Pattern(regexp = AccountPolicy.PASSWORD_PATTERN, message = AccountPolicy.PASSWORD_MESSAGE)
    private String password;

    @NotBlank(message = "Please enter your phone number.")
    @Size(max = 20, message = "Phone number must not exceed 20 characters.")
    @Pattern(regexp = "[+]?[0-9 ()\\-]{8,19}", message = "Please enter a valid phone number.")
    private String phone;

    @NotBlank(message = "Please confirm your password.")
    private String confirmPassword;

    @AssertTrue(message = "You must accept the library terms of use.")
    @Builder.Default
    private boolean termsAccepted = false;
}

