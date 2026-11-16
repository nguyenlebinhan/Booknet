package grp3.java.booknet.repository.user;

import grp3.java.booknet.entity.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    @Query("""
        select u from User u
        where (:admin = true or u.role = grp3.java.booknet.enums.user.UserRole.ROLE_USER)
          and (lower(u.fullName) like lower(concat('%', :keyword, '%'))
            or lower(u.email) like lower(concat('%', :keyword, '%'))
            or lower(u.memberCode) like lower(concat('%', :keyword, '%')))
        """)
    Page<User> searchDirectory(@Param("admin") boolean admin, @Param("keyword") String keyword, Pageable pageable);

    @Query("select count(u) from User u where :admin = true or u.role = grp3.java.booknet.enums.user.UserRole.ROLE_USER")
    long countDirectory(@Param("admin") boolean admin);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.id = :id")
    Optional<User> findLockedById(@Param("id") Long id);

    @Query("select u.id from User u where u.email = :email")
    Optional<Long> findIdByEmail(@Param("email") String email);

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    Optional<User> findByMemberCode(String memberCode);
}
