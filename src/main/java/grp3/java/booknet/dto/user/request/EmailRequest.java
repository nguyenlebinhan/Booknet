package grp3.java.booknet.dto.user.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EmailRequest(
        @NotBlank(message = "Please enter your email address.")
        @Email(message = "Please enter a valid email address.")
        @Size(max = 120, message = "Email address must not exceed 120 characters.") String email) { }