package grp3.java.booknet.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRegisterDto {
    private String fullName;
    private String email;
    private String password;
    private String confirmPassword;
    private Boolean termsAgree;
}
