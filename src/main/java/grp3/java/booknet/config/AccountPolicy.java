package grp3.java.booknet.config;

import java.time.Duration;

public final class AccountPolicy {
    public static final int PASSWORD_MIN_LENGTH = 8;
    public static final int PASSWORD_MAX_LENGTH = 32;
    public static final String PASSWORD_PATTERN = "^(?=.*[a-z])(?=.*[A-Z])(?=.*[0-9]).*$";
    public static final String PASSWORD_MESSAGE = "Mật khẩu phải từ 8–32 ký tự, có chữ hoa, chữ thường và số; tối đa 72 byte UTF-8.";
    public static final Duration RESET_TOKEN_LIFETIME = Duration.ofMinutes(15);
    public static final Duration VERIFICATION_TOKEN_LIFETIME = Duration.ofHours(24);

    private AccountPolicy() { }
}
