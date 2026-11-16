package grp3.java.booknet.dto.user.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ProfileUpdateRequest(
        @NotBlank(message = "Please enter your full name.")
        @Size(min = 2, max = 120, message = "Full name must be between 2 and 120 characters.") String fullName,
        @Size(max = 20, message = "Phone number must not exceed 20 characters.")
        @Pattern(regexp = "^[+0-9() .-]*$", message = "Please enter a valid phone number.") String phone,
        @Size(max = 255, message = "Address must not exceed 255 characters.") String address,
        @Size(max = 500, message = "Avatar URL must not exceed 500 characters.")
        @Pattern(regexp = "^(https?://[^\\s]+)?$", message = "Avatar URL must start with http:// or https:// and contain no spaces.") String avatarUrl) { }
