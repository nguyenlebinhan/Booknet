package grp3.java.booknet.dto.member;
import jakarta.validation.constraints.*;
public record InviteAccountForm(@NotBlank @Size(min=2,max=120) String fullName,
 @NotBlank @Email @Size(max=120) String email,
 @Size(max=20) @Pattern(regexp="|[+]?[0-9 ()-]{8,19}",message="Enter a valid phone number.") String phone,
 @NotNull Long roleId,@Size(max=500) String personalMessage) {}
