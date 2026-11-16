package grp3.java.booknet.security.service;

import grp3.java.booknet.entity.user.User;
import grp3.java.booknet.repository.user.UserRepository;
import grp3.java.booknet.security.dto.CustomUserDetails;
import grp3.java.booknet.service.impl.AccountServiceImpl;
import grp3.java.booknet.enums.user.AuthProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(AccountServiceImpl.normalizeEmail(email))
                .filter(account -> account.getProvider() == AuthProvider.LOCAL && account.getPassword() != null)
                .orElseThrow(() -> new UsernameNotFoundException("No account was found for email: " + email));
        return new CustomUserDetails(user);
    }
}

