package grp3.java.booknet.service.impl;

import grp3.java.booknet.dto.UserRegisterDto;
import grp3.java.booknet.entity.user.User;
import grp3.java.booknet.enums.user.AuthProvider;
import grp3.java.booknet.enums.user.UserStatus;
import grp3.java.booknet.repository.user.UserRepository;
import grp3.java.booknet.service.UserService;
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

    @Override
    @Transactional
    public User registerNewUser(UserRegisterDto registerDto) {
        String normalizedEmail = registerDto.getEmail().trim().toLowerCase();
        log.info("Bắt đầu đăng ký người dùng mới với email: {}", normalizedEmail);

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new IllegalArgumentException("Email này đã được đăng ký trong hệ thống.");
        }

        String defaultUsername = normalizedEmail.split("@")[0];

        User newUser = User.builder()
                .fullName(registerDto.getFullName().trim())
                .email(normalizedEmail)
                .username(defaultUsername)
                .password(passwordEncoder.encode(registerDto.getPassword()))
                .role("ROLE_USER")
                .provider(AuthProvider.LOCAL)
                .enabled(true)
                .emailVerified(true)
                .status(UserStatus.ACTIVE)
                .build();

        return userRepository.save(newUser);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }
        return userRepository.existsByEmail(email.trim().toLowerCase());
    }

    @Override
    @Transactional(readOnly = true)
    public User findByEmail(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }
        return userRepository.findByEmail(email.trim().toLowerCase()).orElse(null);
    }
}
