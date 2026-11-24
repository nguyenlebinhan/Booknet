package grp3.java.booknet.service.impl.account;

import grp3.java.booknet.dto.user.response.UserRegisterDto;
import grp3.java.booknet.entity.user.User;
import grp3.java.booknet.enums.user.AuthProvider;
import grp3.java.booknet.enums.user.UserStatus;
import grp3.java.booknet.enums.user.UserRole;
import grp3.java.booknet.repository.user.UserRepository;
import grp3.java.booknet.service.account.UserService;
import grp3.java.booknet.exception.account.AccountException;
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
    private final grp3.java.booknet.repository.user.RoleDefinitionRepository roles;
    private final PasswordEncoder passwordEncoder;
    private final AccountServiceImpl accountServiceImpl;
    private final grp3.java.booknet.service.member.MemberCardService memberCards;

    @Override
    @Transactional
    public User registerNewUser(UserRegisterDto registerDto) {
        AccountServiceImpl.validateNewPassword(registerDto.getPassword(), registerDto.getConfirmPassword());
        if (registerDto.getFullName() == null || registerDto.getFullName().trim().length() < 2) {
            throw new AccountException(HttpStatus.BAD_REQUEST, "Full name must contain at least 2 characters.");
        }
        String normalizedEmail = AccountServiceImpl.normalizeEmail(registerDto.getEmail());

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new AccountException(HttpStatus.CONFLICT, "This email address is already registered.");
        }

        String defaultUsername = normalizedEmail.split("@")[0];

        User newUser = User.builder()
                .fullName(registerDto.getFullName().trim())
                .email(normalizedEmail)
                .phone(registerDto.getPhone() == null ? null : registerDto.getPhone().trim())
                .username(defaultUsername.substring(0, Math.min(50, defaultUsername.length())))
                .password(passwordEncoder.encode(registerDto.getPassword()))
                .role(UserRole.ROLE_USER)
                .provider(AuthProvider.LOCAL)
                .enabled(true)
                .emailVerified(false)
                .status(UserStatus.ACTIVE)
                .build();

        User saved = userRepository.saveAndFlush(newUser);
        roles.findBySystemRole(UserRole.ROLE_USER).ifPresent(saved::setAssignedRole);
        memberCards.createPending(saved);
        accountServiceImpl.sendInitialVerification(saved);
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }
        return userRepository.existsByEmail(AccountServiceImpl.normalizeEmail(email));
    }

    @Override
    @Transactional(readOnly = true)
    public User findByEmail(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }
        return userRepository.findByEmail(AccountServiceImpl.normalizeEmail(email)).orElse(null);
    }
}
