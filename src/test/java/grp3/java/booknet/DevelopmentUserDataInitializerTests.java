package grp3.java.booknet;

import grp3.java.booknet.config.development.DevelopmentUserDataInitializer;
import grp3.java.booknet.entity.user.User;
import grp3.java.booknet.enums.user.AuthProvider;
import grp3.java.booknet.enums.user.UserRole;
import grp3.java.booknet.enums.user.UserStatus;
import grp3.java.booknet.repository.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DevelopmentUserDataInitializerTests {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Test
    void createsConfiguredLocalAdminLibrarianAndMemberAccounts() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenAnswer(invocation -> "encoded:" + invocation.getArgument(0));
        DevelopmentUserDataInitializer initializer = initializer(
                " ADMIN@booknet.local ", "AdminPass1",
                "librarian@booknet.local", "LibrarianPass1",
                "user@booknet.local", "UserPass1");
        initializer.run();

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository, times(3)).save(userCaptor.capture());
        assertThat(userCaptor.getAllValues()).extracting(User::getEmail)
                .containsExactly("admin@booknet.local", "librarian@booknet.local", "user@booknet.local");
        assertThat(userCaptor.getAllValues()).extracting(User::getRole)
                .containsExactly(UserRole.ROLE_ADMIN, UserRole.ROLE_LIBRARIAN, UserRole.ROLE_USER);
        assertThat(userCaptor.getAllValues()).allSatisfy(user -> {
            assertThat(user.getProvider()).isEqualTo(AuthProvider.LOCAL);
            assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
            assertThat(user.isEnabled()).isTrue();
            assertThat(user.isEmailVerified()).isTrue();
        });
        assertThat(userCaptor.getAllValues()).extracting(User::getPassword)
                .containsExactly("encoded:AdminPass1", "encoded:LibrarianPass1", "encoded:UserPass1");
    }

    @Test
    void doesNotModifyAnyAccountThatAlreadyExists() {
        when(userRepository.existsByEmail(anyString())).thenReturn(true);
        DevelopmentUserDataInitializer initializer = initializer(
                "admin@booknet.local", "AdminPass1",
                "librarian@booknet.local", "LibrarianPass1",
                "user@booknet.local", "UserPass1");

        initializer.run();

        verify(userRepository, times(3)).existsByEmail(anyString());
        verify(userRepository, never()).save(any(User.class));
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    void doesNothingWhenSeedCredentialsAreNotConfigured() {
        initializer("", "", "", "", "", "").run();

        verifyNoInteractions(userRepository, passwordEncoder);
    }

    @Test
    void rejectsIncompleteOrInvalidCredentialsBeforeSavingAnyAccount() {
        DevelopmentUserDataInitializer incomplete = initializer(
                "", "", "", "", "user@booknet.local", "");
        assertThatThrownBy(incomplete::run)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Both email and password");

        DevelopmentUserDataInitializer invalidPassword = initializer(
                "admin@booknet.local", "AdminPass1",
                "", "",
                "user@booknet.local", "weak");
        assertThatThrownBy(invalidPassword::run)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("configured account password policy");

        verifyNoInteractions(userRepository, passwordEncoder);
    }

    private DevelopmentUserDataInitializer initializer(
            String adminEmail, String adminPassword,
            String librarianEmail, String librarianPassword,
            String userEmail, String userPassword) {
        return new DevelopmentUserDataInitializer(
                userRepository, passwordEncoder, adminEmail, adminPassword,
                librarianEmail, librarianPassword, userEmail, userPassword);
    }
}
