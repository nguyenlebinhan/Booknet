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

    @NotBlank(message = "Họ và tên không được để trống")
    @Size(min = 2, max = 120, message = "Họ và tên phải từ 2 đến 120 ký tự")
    private String fullName;

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không đúng định dạng")
    @Size(max = 120, message = "Email tối đa 120 ký tự")
    private String email;

    @NotBlank(message = "Mật khẩu không được để trống")
    @Size(min = AccountPolicy.PASSWORD_MIN_LENGTH, max = AccountPolicy.PASSWORD_MAX_LENGTH, message = AccountPolicy.PASSWORD_MESSAGE)
    @Pattern(regexp = AccountPolicy.PASSWORD_PATTERN, message = AccountPolicy.PASSWORD_MESSAGE)
    private String password;

    @NotBlank(message = "Vui lòng nhập số điện thoại")
    @Size(max = 20, message = "Số điện thoại tối đa 20 ký tự")
    @Pattern(regexp = "[+]?[0-9 ()\\-]{8,19}", message = "Số điện thoại không hợp lệ")
    private String phone;

    @NotBlank(message = "Vui lòng xác nhận lại mật khẩu")
    private String confirmPassword;

    @AssertTrue(message = "Bạn cần đồng ý với Điều khoản sử dụng của thư viện")
    @Builder.Default
    private boolean termsAccepted = false;
}

