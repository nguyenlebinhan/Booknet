package grp3.java.booknet.config;

import java.time.Duration;

public final class AccountPolicy {
    public static final int PASSWORD_MIN_LENGTH = 8;
    public static final int PASSWORD_MAX_LENGTH = 32;
    public static final String PASSWORD_PATTERN = "^(?=.*[a-z])(?=.*[A-Z])(?=.*[0-9]).*$";
    public static final String PASSWORD_MESSAGE = "Password must be 8–32 characters and include uppercase and lowercase letters and a number; maximum 72 UTF-8 bytes.";
    public static final Duration RESET_TOKEN_LIFETIME = Duration.ofMinutes(15);
    public static final Duration VERIFICATION_TOKEN_LIFETIME = Duration.ofHours(24);

    private AccountPolicy() { }
}
