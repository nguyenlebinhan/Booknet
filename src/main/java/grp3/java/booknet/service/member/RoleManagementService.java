package grp3.java.booknet.service.member;

import grp3.java.booknet.entity.user.RoleDefinition;
import grp3.java.booknet.enums.user.UserRole;
import grp3.java.booknet.repository.user.RoleDefinitionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

/** Initializes system roles and exposes the read-only role directory. */
@Service
@RequiredArgsConstructor
public class RoleManagementService {
    private final RoleDefinitionRepository roles;

    @Transactional
    public void initialize() {
        for (var role : UserRole.values()) {
            if (roles.findBySystemRole(role).isPresent()) continue;
            var definition = new RoleDefinition();
            definition.setName(switch (role) {
                case ROLE_USER -> "Member";
                case ROLE_ADMIN -> "Administrator";
                case ROLE_LIBRARIAN -> "Librarian";
                case ROLE_GUEST -> "Guest";
            });
            definition.setSystemRole(role);
            definition.setDescription(switch (role) {
                case ROLE_USER -> "Manage a personal profile and borrowing requests. No access to other members or staff tools.";
                case ROLE_LIBRARIAN -> "Work with the catalog, circulation and all-member service records. No role or policy administration.";
                case ROLE_ADMIN -> "Manage library operations with protected access to user accounts, roles, policies and the audit log.";
                case ROLE_GUEST -> "Browse public pages without an account.";
            });
            definition.setCapabilities(new HashSet<>(UserRole.defaults(role)));
            roles.save(definition);
        }
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN')")
    public List<RoleDefinition> all() {
        return roles.findAll().stream().sorted(Comparator.comparing(RoleDefinition::getId)).toList();
    }
}
