package grp3.java.booknet.repository.user;

import grp3.java.booknet.entity.user.MemberCard;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberCardRepository extends JpaRepository<MemberCard, Long> {
    java.util.Optional<MemberCard> findByUserId(Long userId);
    java.util.Optional<MemberCard> findByUser_Id(Long userId);

}
