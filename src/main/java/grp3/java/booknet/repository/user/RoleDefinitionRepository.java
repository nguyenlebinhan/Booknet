package grp3.java.booknet.repository.user;
import grp3.java.booknet.entity.user.RoleDefinition;
import grp3.java.booknet.enums.user.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface RoleDefinitionRepository extends JpaRepository<RoleDefinition,Long>{
 Optional<RoleDefinition> findBySystemRole(UserRole role);
 boolean existsByNameIgnoreCase(String name);
}
