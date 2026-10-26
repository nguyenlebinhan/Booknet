package grp3.java.booknet.service;

import grp3.java.booknet.dto.UserLoginDto;
import grp3.java.booknet.dto.UserRegisterDto;
import grp3.java.booknet.dto.UserSessionDto;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

@Service
public class AuthServiceImpl implements AuthService {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");

    private final Map<String, StoredUser> usersByEmail = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(100);

    public AuthServiceImpl() {
        // Pre-populate demo accounts for easy testing
        StoredUser demoStudent = new StoredUser(
                1L,
                "Nguyễn Văn Độc Giả",
                "student@library.edu.vn",
                "123456",
                "BN-2026-001",
                "READER"
        );
        StoredUser demoAdmin = new StoredUser(
                2L,
                "Quản Trị Viên BookNet",
                "admin@booknet.vn",
                "admin123",
                "BN-ADMIN-01",
                "ADMIN"
        );
        usersByEmail.put(demoStudent.email.toLowerCase(), demoStudent);
        usersByEmail.put(demoAdmin.email.toLowerCase(), demoAdmin);
    }

    @Override
    public UserSessionDto register(UserRegisterDto registerDto) {
        if (registerDto == null) {
            throw new IllegalArgumentException("Dữ liệu đăng ký không hợp lệ!");
        }

        String fullName = registerDto.getFullName() != null ? registerDto.getFullName().trim() : "";
        String email = registerDto.getEmail() != null ? registerDto.getEmail().trim().toLowerCase() : "";
        String password = registerDto.getPassword();
        String confirmPassword = registerDto.getConfirmPassword();

        if (fullName.isEmpty()) {
            throw new IllegalArgumentException("Họ và tên không được để trống!");
        }

        if (email.isEmpty() || !EMAIL_PATTERN.matcher(email).matches()) {
            throw new IllegalArgumentException("Địa chỉ email không đúng định dạng!");
        }

        if (password == null || password.length() < 6) {
            throw new IllegalArgumentException("Mật khẩu phải chứa ít nhất 6 ký tự!");
        }

        if (!password.equals(confirmPassword)) {
            throw new IllegalArgumentException("Mật khẩu xác nhận không khớp!");
        }

        if (usersByEmail.containsKey(email)) {
            throw new IllegalArgumentException("Email này đã được đăng ký tài khoản!");
        }

        long id = idGenerator.incrementAndGet();
        String memberCode = "BN-" + String.format("%04d", id);
        StoredUser newUser = new StoredUser(id, fullName, email, password, memberCode, "READER");
        usersByEmail.put(email, newUser);

        return UserSessionDto.builder()
                .id(newUser.id)
                .fullName(newUser.fullName)
                .email(newUser.email)
                .memberCode(newUser.memberCode)
                .roleName(newUser.roleName)
                .build();
    }

    @Override
    public UserSessionDto login(UserLoginDto loginDto) {
        if (loginDto == null) {
            throw new IllegalArgumentException("Dữ liệu đăng nhập không hợp lệ!");
        }

        String email = loginDto.getEmail() != null ? loginDto.getEmail().trim().toLowerCase() : "";
        String password = loginDto.getPassword();

        if (email.isEmpty()) {
            throw new IllegalArgumentException("Vui lòng nhập địa chỉ email hoặc mã độc giả!");
        }

        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("Vui lòng nhập mật khẩu!");
        }

        StoredUser user = usersByEmail.get(email);
        if (user == null || !user.password.equals(password)) {
            throw new IllegalArgumentException("Email hoặc mật khẩu không chính xác!");
        }

        return UserSessionDto.builder()
                .id(user.id)
                .fullName(user.fullName)
                .email(user.email)
                .memberCode(user.memberCode)
                .roleName(user.roleName)
                .build();
    }

    private static class StoredUser {
        final Long id;
        final String fullName;
        final String email;
        final String password;
        final String memberCode;
        final String roleName;

        StoredUser(Long id, String fullName, String email, String password, String memberCode, String roleName) {
            this.id = id;
            this.fullName = fullName;
            this.email = email;
            this.password = password;
            this.memberCode = memberCode;
            this.roleName = roleName;
        }
    }
}
