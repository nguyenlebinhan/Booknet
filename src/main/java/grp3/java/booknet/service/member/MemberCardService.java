package grp3.java.booknet.service.member;

import grp3.java.booknet.dto.member.MemberForms.ActivationForm;
import grp3.java.booknet.entity.user.*;
import grp3.java.booknet.enums.user.*;
import grp3.java.booknet.repository.user.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.*;
import java.util.UUID;

@Service @RequiredArgsConstructor
public class MemberCardService {
    private final UserRepository users;
    private final MemberCardRepository cards;
    private final Clock clock;

    /** Called only by trusted registration flows inside their transaction. */
    @Transactional
    public void createPending(User user) {
        if (user.getRole() != UserRole.ROLE_USER || user.getMemberCard() != null) return;
        users.findLockedById(user.getId()).ifPresentOrElse(current -> ensurePendingCard(current),
                () -> ensurePendingCard(user));
    }

    @Transactional
    public void ensurePending(Long userId) {
        User user = users.findLockedById(userId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        ensurePendingCard(user);
        users.saveAndFlush(user);
    }

    private void ensurePendingCard(User user) {
        if (user.getRole() != UserRole.ROLE_USER || (user.getAssignedRole()!=null && user.getAssignedRole().getSystemRole()!=UserRole.ROLE_USER)) return;
        if (user.getMemberCard() != null) return;
        MemberCard card = user.getMemberCard();
        if (card == null) card = cards.findByUserId(user.getId()).orElse(null);
        if (card == null) {
            card = new MemberCard();
            card.setUser(user);
            card.setCardNumber("BN-" + UUID.randomUUID().toString().replace("-", "").substring(0, 20).toUpperCase());
            card = cards.saveAndFlush(card);
        }
        user.setMemberCard(card);
        users.saveAndFlush(user);
    }

    @Transactional(readOnly = true)
    public grp3.java.booknet.dto.member.MemberDirectoryView.Person ownCard(Long id) {
        return grp3.java.booknet.dto.member.MemberDirectoryView.Person.from(users.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND)), LocalDate.now(clock));
    }

    @Transactional
    @PreAuthorize("hasAuthority('MEMBER_CARD_ACTIVATE')")
    public void activate(Long actorId, Long id, ActivationForm form) {
        User actor = users.findById(actorId).orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN));
        if (!actor.isEnabled() || !actor.isEmailVerified() || actor.getStatus() != UserStatus.ACTIVE
                || actor.effectiveAuthorities().stream().noneMatch(a -> a.getAuthority().equals("MEMBER_CARD_ACTIVATE")))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        User user = users.findLockedById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (user.getRole() != UserRole.ROLE_USER || !user.isEnabled() || !user.isEmailVerified()
                || user.getStatus() != UserStatus.ACTIVE)
            throw new IllegalArgumentException("Only active, email-verified member accounts can activate a card.");
        LocalDate today = LocalDate.now(clock);
        if (!form.identityChecked() || form.expiresOn() == null || !form.expiresOn().isAfter(today))
            throw new IllegalArgumentException("Check identity documents and enter a future expiry date.");
        createPending(user);
        MemberCard card = user.getMemberCard();
        if (card.getIssuedOn() != null) throw new IllegalArgumentException("This card has already been activated. Renewal requires a separate process.");
        card.setIssuedOn(today);
        card.setExpiresOn(form.expiresOn());
        card.setActivatedBy(actorId);
    }
}
