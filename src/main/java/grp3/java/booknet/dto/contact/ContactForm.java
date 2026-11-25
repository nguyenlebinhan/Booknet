package grp3.java.booknet.dto.contact;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ContactForm(
        @NotBlank(message = "Please enter your full name.")
        @Size(min = 2, max = 120, message = "Full name must be between 2 and 120 characters.") String fullName,
        @NotBlank(message = "Please enter your email address.")
        @Email(message = "Please enter a valid email address.")
        @Size(max = 120, message = "Email address must not exceed 120 characters.")
        @Pattern(regexp = "^[^\\r\\n]*$", message = "Please enter a valid email address.") String email,
        @Size(max = 20, message = "Phone number must not exceed 20 characters.")
        @Pattern(regexp = "|[+]?[0-9 ()-]{8,19}", message = "Please enter a valid phone number.") String phone,
        @Pattern(regexp = "^(Borrowing & renewals|Membership & card activation|Catalog searches & reservations|Book donations|Events & reading rooms|Other)?$",
                message = "Please select a valid topic.") String subject,
        @NotBlank(message = "Please enter your message.")
        @Size(max = 5000, message = "Message must not exceed 5000 characters.") String message) {

    public ContactForm {
        fullName = fullName == null ? "" : fullName.trim();
        email = email == null ? "" : email.trim();
        phone = phone == null ? "" : phone.trim();
        subject = subject == null ? "" : subject.trim();
        message = message == null ? "" : message.trim();
    }
}
