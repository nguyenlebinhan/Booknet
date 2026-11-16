package grp3.java.booknet.config.development;

import grp3.java.booknet.config.account.AccountPolicy;
import grp3.java.booknet.entity.user.User;
import grp3.java.booknet.enums.user.AuthProvider;
import grp3.java.booknet.enums.user.UserRole;
import grp3.java.booknet.enums.user.UserStatus;
import grp3.java.booknet.repository.user.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@Slf4j
@Component
@Profile("local")
public class DevelopmentUserDataInitializer implements CommandLineRunner {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminEmail;
    private final String adminPassword;
    private final String librarianEmail;
    private final String librarianPassword;
    private final String userEmail;
    private final String userPassword;

    public DevelopmentUserDataInitializer(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${BOOKNET_SEED_ADMIN_EMAIL:}") String adminEmail,
            @Value("${BOOKNET_SEED_ADMIN_PASSWORD:}") String adminPassword,
            @Value("${BOOKNET_SEED_LIBRARIAN_EMAIL:}") String librarianEmail,
            @Value("${BOOKNET_SEED_LIBRARIAN_PASSWORD:}") String librarianPassword,
            @Value("${BOOKNET_SEED_USER_EMAIL:}") String userEmail,
            @Value("${BOOKNET_SEED_USER_PASSWORD:}") String userPassword) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
        this.librarianEmail = librarianEmail;
        this.librarianPassword = librarianPassword;
        this.userEmail = userEmail;
        this.userPassword = userPassword;
    }

    @Override
    public void run(String... args) {
        List<SeedAccount> accounts = new ArrayList<>();
        addIfConfigured(accounts, "Library Administrator", adminEmail, adminPassword, UserRole.ROLE_ADMIN);
        addIfConfigured(accounts, "Library Librarian", librarianEmail, librarianPassword, UserRole.ROLE_LIBRARIAN);
        addIfConfigured(accounts, "Booknet Member", userEmail, userPassword, UserRole.ROLE_USER);

        if (accounts.isEmpty()) {
            log.info("Development account seeding is disabled; seed credentials are not configured.");
            return;
        }

        for (SeedAccount account : accounts) {
            seedIfMissing(account);
        }
    }

    private static void addIfConfigured(List<SeedAccount> accounts, String fullName,
                                        String email, String password, UserRole role) {
        String normalizedEmail = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        String configuredPassword = password == null ? "" : password;

        if (normalizedEmail.isEmpty() && configuredPassword.isEmpty()) {
            return;
        }
        if (normalizedEmail.isEmpty() || configuredPassword.isEmpty()) {
            throw new IllegalStateException("Both email and password must be configured for the " + role + " seed account.");
        }
        if (!EMAIL_PATTERN.matcher(normalizedEmail).matches()) {
            throw new IllegalStateException("The configured email for the " + role + " seed account is invalid.");
        }
        validatePassword(configuredPassword, role);
        accounts.add(new SeedAccount(fullName, normalizedEmail, configuredPassword, role));
    }

    private static void validatePassword(String password, UserRole role) {
        int length = password.length();
        int byteLength = password.getBytes(StandardCharsets.UTF_8).length;
        if (length < AccountPolicy.PASSWORD_MIN_LENGTH
                || length > AccountPolicy.PASSWORD_MAX_LENGTH
                || byteLength > 72
                || !password.matches(AccountPolicy.PASSWORD_PATTERN)) {
            throw new IllegalStateException("The password for the " + role
                    + " seed account must follow the configured account password policy.");
        }
    }

    private void seedIfMissing(SeedAccount account) {
        if (userRepository.existsByEmail(account.email())) {
            log.info("Skipped existing {} seed account.", account.role());
            return;
        }

        String username = account.email().substring(0, account.email().indexOf('@'));
        if (username.length() > 50) {
            username = username.substring(0, 50);
        }

        User user = User.builder()
                .fullName(account.fullName())
                .email(account.email())
                .username(username)
                .password(passwordEncoder.encode(account.password()))
                .role(account.role())
                .provider(AuthProvider.LOCAL)
                .enabled(true)
                .emailVerified(true)
                .status(UserStatus.ACTIVE)
                .build();
        userRepository.save(user);
        log.info("Created local {} seed account.", account.role());
    }

    private record SeedAccount(String fullName, String email, String password, UserRole role) {
    }
}
