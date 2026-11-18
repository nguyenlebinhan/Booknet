package grp3.java.booknet.service.member;

import grp3.java.booknet.entity.user.AccountAccessAudit;
import grp3.java.booknet.entity.user.User;
import grp3.java.booknet.enums.user.UserRole;
import grp3.java.booknet.enums.user.UserStatus;
import grp3.java.booknet.repository.user.AccountAccessAuditRepository;
import grp3.java.booknet.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service @RequiredArgsConstructor
public class AccountAdministrationService {
    private final UserRepository users;
    private final AccountAccessAuditRepository audits;
    private final grp3.java.booknet.repository.user.RoleDefinitionRepository roles;
    private final grp3.java.booknet.service.impl.account.AccountServiceImpl accountFlows;
    private final grp3.java.booknet.service.account.AccountService accounts;

    @Transactional
    @PreAuthorize("hasAuthority('USER_ACCESS_UPDATE')")
    public void changeAccess(Long actor, Long target, boolean enabled, Long revision) {
        User user = target(actor, target, revision);
        UserStatus status = enabled ? UserStatus.ACTIVE : UserStatus.SUSPENDED;
        if (user.isEnabled() == enabled && user.getStatus() == status) return;
        audit(actor, user, "ACCESS", user.isEnabled() + "/" + user.getStatus(), enabled + "/" + status);
        user.setEnabled(enabled);
        user.setStatus(status);
        user.setAuthVersion(user.getAuthVersion() + 1);
    }

    @Transactional
    @PreAuthorize("hasAuthority('USER_ACCESS_UPDATE')")
    public void changeStatus(Long actor, Long target, UserStatus status, Long revision) {
        if (status == null) throw new IllegalArgumentException("Choose a valid account status.");
        User user = target(actor, target, revision);
        if (user.getStatus() == status && user.isEnabled() == (status != UserStatus.SUSPENDED)) return;
        if (status == UserStatus.ACTIVE && !user.isEmailVerified())
            throw new IllegalArgumentException("Verify the member's email before activating account access.");
        String previous = user.getStatus().name() + "/" + user.isEnabled();
        audit(actor, user, "STATUS", previous, status.name() + "/" + (status != UserStatus.SUSPENDED));
        user.setStatus(status);
        user.setEnabled(status != UserStatus.SUSPENDED);
        user.setAuthVersion(user.getAuthVersion() + 1);
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('USER_ACCESS_UPDATE')")
    public Long inviteAccount(Long actorId,grp3.java.booknet.dto.member.InviteAccountForm form) {
        User actor=users.findLockedById(actorId).orElseThrow(()->new ResponseStatusException(HttpStatus.FORBIDDEN));
        if(actor.getRole()!=UserRole.ROLE_ADMIN || !actor.isEnabled() || !actor.isEmailVerified() || actor.getStatus()!=UserStatus.ACTIVE)throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        String email=grp3.java.booknet.service.impl.account.AccountServiceImpl.normalizeEmail(form.email());
        if(users.existsByEmail(email))throw new IllegalArgumentException("This email address is already registered.");
        var role=roles.findById(form.roleId()).orElseThrow(()->new IllegalArgumentException("Select a valid role."));
        if(role.getSystemRole()==UserRole.ROLE_GUEST)throw new IllegalArgumentException("Guest is public access without an account.");
        User user=User.builder().fullName(form.fullName().trim()).email(email).phone(form.phone()==null?null:form.phone().trim())
             .role(role.getSystemRole()==null?UserRole.ROLE_USER:role.getSystemRole()).assignedRole(role)
             .provider(grp3.java.booknet.enums.user.AuthProvider.LOCAL).enabled(true).emailVerified(false).status(UserStatus.ACTIVE).build();
        users.saveAndFlush(user);accountFlows.invite(user,form.personalMessage());
        audit(actorId,user,"ACCOUNT_INVITE","NONE",role.getName());return user.getId();
    }

    @Transactional @PreAuthorize("hasRole('ADMIN') and hasAuthority('USER_ACCESS_UPDATE')")
    public void control(Long actor,Long target,String action,grp3.java.booknet.dto.member.AdminControlForm form) {
        if(!Boolean.TRUE.equals(form.confirmed()) || form.reason()==null || form.reason().isBlank())throw new IllegalArgumentException("A reason and confirmation are required.");
        User user=target(actor,target,form.revision());
        switch(action){
        case "lock" -> {user.setEnabled(false);user.setStatus(UserStatus.SUSPENDED);user.setAuthVersion(user.getAuthVersion()+1);}
        case "unlock" -> {if(!user.isEmailVerified())throw new IllegalArgumentException("Email verification is required.");user.setEnabled(true);user.setStatus(UserStatus.ACTIVE);user.setAuthVersion(user.getAuthVersion()+1);}
        case "sessions" -> user.setAuthVersion(user.getAuthVersion()+1);
        case "reset" -> {if(!user.isEmailVerified())throw new IllegalArgumentException("Only activated accounts can receive a reset link.");accountFlows.requestAdminReset(user.getId());}
        case "invite" -> accountFlows.invite(user,null);
        default -> throw new IllegalArgumentException("Unknown account action.");
        }
        var entry=new AccountAccessAudit();entry.setActorId(actor);entry.setTargetId(target);entry.setAction("ACCOUNT_"+action.toUpperCase(java.util.Locale.ROOT));entry.setPreviousValue("revision:"+form.revision());entry.setNewValue(user.getStatus().name());entry.setReason(form.reason().trim());audits.save(entry);
    }
    @Transactional @PreAuthorize("hasRole('ADMIN') and hasAuthority('USER_ACCESS_UPDATE')")
    public void updateProfile(Long actor,Long id,grp3.java.booknet.dto.user.request.ProfileUpdateRequest form){
        User user=users.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
        target(actor,id,user.getAuthVersion());accounts.updateProfile(id,form);audit(actor,user,"PROFILE","Previous profile","Updated profile");
    }
    private User target(Long actorId, Long targetId, Long revision) {
        // Serialize admin mutations in a stable order; protect the last administrator from concurrent edits.
        var administrators = users.lockAdministrators();
        User actor = administrators.stream().filter(a -> a.getId().equals(actorId) && a.isEnabled()
                && a.isEmailVerified() && a.getStatus() == UserStatus.ACTIVE).findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN));
        if (actor.getId().equals(targetId)) throw new IllegalArgumentException("You cannot change your own role or access.");
        User target = users.findLockedById(targetId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (target.getRole() != UserRole.ROLE_USER && target.getRole() != UserRole.ROLE_LIBRARIAN)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        if (revision == null || target.getAuthVersion() != revision)
            throw new IllegalArgumentException("This account changed. Reload the details before trying again.");
        return target;
    }

    private void audit(Long actor, User user, String action, String previous, String value) {
        var entry = new AccountAccessAudit();
        entry.setActorId(actor); entry.setTargetId(user.getId()); entry.setAction(action);
        entry.setPreviousValue(previous); entry.setNewValue(value);
        audits.save(entry);
    }
}
