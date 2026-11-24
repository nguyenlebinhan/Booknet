package grp3.java.booknet.service.member;

import grp3.java.booknet.enums.user.UserRole;
import grp3.java.booknet.enums.user.UserStatus;
import grp3.java.booknet.repository.user.UserRepository;
import grp3.java.booknet.repository.user.MemberCardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Read-only eligibility checks for borrowing and other library workflows. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberEligibilityService {
    private final UserRepository users;
    private final MemberCardRepository cards;
    private final java.time.Clock clock;

    public boolean isEligible(Long userId) {
        return users.findById(userId).map(user -> user.getRole() == UserRole.ROLE_USER && (user.getAssignedRole()==null || user.getAssignedRole().getSystemRole()==UserRole.ROLE_USER)
                && user.isEnabled() && user.isEmailVerified() && user.getStatus() == UserStatus.ACTIVE
                && cards.findByUserId(userId).map(card -> "Active".equals(card.status(java.time.LocalDate.now(clock))))
                        .orElse(false)).orElse(false);
    }

    public boolean isEligibleByMemberCode(String memberCode) {
        return memberCode != null && users.findIdByMemberCode(memberCode)
                .map(this::isEligible).orElse(false);
    }
}
