package grp3.java.booknet.service.impl;

import grp3.java.booknet.dto.UserRegisterDto;
import grp3.java.booknet.entity.user.User;
import grp3.java.booknet.enums.user.AuthProvider;
import grp3.java.booknet.enums.user.UserStatus;
import grp3.java.booknet.enums.user.UserRole;
import grp3.java.booknet.repository.user.UserRepository;
import grp3.java.booknet.service.UserService;
import grp3.java.booknet.service.AccountService;
import grp3.java.booknet.exception.AccountException;
import org.springframework.http.HttpStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AccountService accountService;

    @Override
    @Transactional
    public User registerNewUser(UserRegisterDto registerDto) {
        AccountService.validateNewPassword(registerDto.getPassword(), registerDto.getConfirmPassword());
        if (registerDto.getFullName() == null || registerDto.getFullName().trim().length() < 2) {
            throw new AccountException(HttpStatus.BAD_REQUEST, "Họ tên phải có ít nhất 2 ký tự.");
        }
        String normalizedEmail = AccountService.normalizeEmail(registerDto.getEmail());

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new AccountException(HttpStatus.CONFLICT, "Email này đã được đăng ký trong hệ thống.");
        }

        String defaultUsername = normalizedEmail.split("@")[0];

        User newUser = User.builder()
                .fullName(registerDto.getFullName().trim())
                .email(normalizedEmail)
                .username(defaultUsername.substring(0, Math.min(50, defaultUsername.length())))
                .password(passwordEncoder.encode(registerDto.getPassword()))
                .role(UserRole.ROLE_USER)
                .provider(AuthProvider.LOCAL)
                .enabled(true)
                .emailVerified(false)
                .status(UserStatus.ACTIVE)
                .build();

        User saved = userRepository.saveAndFlush(newUser);
        accountService.sendInitialVerification(saved);
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }
        return userRepository.existsByEmail(AccountService.normalizeEmail(email));
    }

    @Override
    @Transactional(readOnly = true)
    public User findByEmail(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }
        return userRepository.findByEmail(AccountService.normalizeEmail(email)).orElse(null);
    }
}
