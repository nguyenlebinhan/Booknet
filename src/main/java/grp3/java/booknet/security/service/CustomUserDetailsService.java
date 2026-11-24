package grp3.java.booknet.security.service;

import grp3.java.booknet.entity.user.User;
import grp3.java.booknet.repository.user.UserRepository;
import grp3.java.booknet.security.dto.CustomUserDetails;
import grp3.java.booknet.service.impl.account.AccountServiceImpl;
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
    private final grp3.java.booknet.repository.user.RoleDefinitionRepository roles;

    @Override
    @Transactional
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(AccountServiceImpl.normalizeEmail(email))
                .filter(User::hasPassword)
                .orElseThrow(() -> new UsernameNotFoundException("No account was found for email: " + email));
        if(user.getAssignedRole()==null)roles.findBySystemRole(user.getRole()).ifPresent(user::setAssignedRole);
        return new CustomUserDetails(user);
    }
}
