package grp3.java.booknet.service.impl.account;

import grp3.java.booknet.config.account.AccountPolicy;
import grp3.java.booknet.dto.user.response.ProfileResponse;
import grp3.java.booknet.dto.user.request.ChangePasswordRequest;
import grp3.java.booknet.dto.user.request.ProfileUpdateRequest;
import grp3.java.booknet.dto.user.request.ResetPasswordRequest;
import grp3.java.booknet.entity.user.AccountToken;
import grp3.java.booknet.entity.user.User;
import grp3.java.booknet.enums.user.AccountTokenPurpose;
import grp3.java.booknet.enums.user.AuthProvider;
import grp3.java.booknet.enums.user.UserStatus;
import grp3.java.booknet.exception.account.AccountException;
import grp3.java.booknet.repository.user.AccountTokenRepository;
import grp3.java.booknet.repository.user.OAuthIdentityRepository;
import grp3.java.booknet.repository.user.UserRepository;
import grp3.java.booknet.service.account.AccountMailService;
import grp3.java.booknet.service.account.AccountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private final UserRepository users;
    private final AccountTokenRepository tokens;
    private final OAuthIdentityRepository identities;
    private final PasswordEncoder passwords;
    private final AccountMailService mail;
    private final Clock clock;

    public static String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    public static void validateNewPassword(String password, String confirmation) {
        if (password == null || password.length() < AccountPolicy.PASSWORD_MIN_LENGTH
                || password.length() > AccountPolicy.PASSWORD_MAX_LENGTH
                || !password.matches(AccountPolicy.PASSWORD_PATTERN)
                || password.getBytes(StandardCharsets.UTF_8).length > 72 || !password.equals(confirmation)) {
            throw new AccountException(HttpStatus.BAD_REQUEST, AccountPolicy.PASSWORD_MESSAGE + " Password confirmation must match.");
        }
    }

    @Transactional
    public void sendInitialVerification(User user) {
        try {
            issueToken(user, AccountTokenPurpose.VERIFY_EMAIL);
        } catch (MailException ex) {
            throw new AccountException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Unable to send the verification email right now. Please try again later.");
        }
    }

    @Transactional
    public void requestEmail(String email, AccountTokenPurpose purpose) {
        // Return the same HTTP result for unknown, Google-only and ineligible accounts.
        users.findIdByEmail(normalizeEmail(email)).ifPresent(id -> {
            User user = lockUser(id);
            if (!user.isEnabled() || user.getStatus() != UserStatus.ACTIVE
                    || (purpose == AccountTokenPurpose.VERIFY_EMAIL
                        && (user.getProvider() != AuthProvider.LOCAL || !user.hasPassword() || user.isEmailVerified()))
                    || (purpose == AccountTokenPurpose.RESET_PASSWORD && !user.isEmailVerified())) {
                return;
            }
            try {
                issueToken(user, purpose);
            } catch (MailException ex) {
                // Do not reveal account existence when SMTP is unavailable, or log the token/email.
                log.warn("Account email delivery failed; check SMTP configuration ({})", ex.getClass().getSimpleName());
                invalidateTokens(user.getId(), purpose);
            }
        });
    }

    @Transactional
    public void requestPasswordSetup(Long userId) {
        User user = lockUser(userId);
        requirePasswordEligible(user);
        if (user.getProvider() != AuthProvider.GOOGLE || user.hasPassword()) {
            throw new AccountException(HttpStatus.BAD_REQUEST,
                    "Password setup is only available for Google accounts without a password.");
        }
        try {
            issueToken(user, AccountTokenPurpose.RESET_PASSWORD);
        } catch (MailException ex) {
            log.warn("Password setup email delivery failed; check SMTP configuration ({})",
                    ex.getClass().getSimpleName());
            throw new AccountException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Unable to send the password setup email right now. Please try again later.");
        }
    }

    @Transactional
    public void verifyEmail(String rawToken) {
        AccountToken token = lockValidToken(rawToken, AccountTokenPurpose.VERIFY_EMAIL);
        User user = token.getUser();
        requireLocalActive(user);
        if(!user.hasPassword())throw invalidToken();
        user.setEmailVerified(true);
        invalidateTokens(user.getId(), AccountTokenPurpose.VERIFY_EMAIL);
    }

    @Transactional(readOnly=true)
    public boolean isInvitation(String rawToken){
        if(rawToken==null || rawToken.isBlank() || rawToken.length()>200)return false;
        return tokens.findPurposeByHash(hash(rawToken)).map(p->p==AccountTokenPurpose.INVITE_ACCOUNT).orElse(false);
    }
    @Transactional(readOnly = true)
    public void validateResetToken(String rawToken) {
        AccountToken token = tokens.findByTokenHash(hash(rawToken)).orElseThrow(AccountServiceImpl::invalidToken);
        if(token.getPurpose()==AccountTokenPurpose.INVITE_ACCOUNT){
            validateToken(token,AccountTokenPurpose.INVITE_ACCOUNT);requireLocalActive(token.getUser());
            if(token.getUser().hasPassword() || token.getUser().isEmailVerified())throw invalidToken();
        }else{validateToken(token,AccountTokenPurpose.RESET_PASSWORD);requirePasswordEligible(token.getUser());}
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        validateNewPassword(request.newPassword(), request.confirmPassword());
        var purpose=tokens.findPurposeByHash(hash(request.token())).orElseThrow(AccountServiceImpl::invalidToken);
        boolean invitation=purpose==AccountTokenPurpose.INVITE_ACCOUNT;
        AccountToken token=lockValidToken(request.token(),invitation?AccountTokenPurpose.INVITE_ACCOUNT:AccountTokenPurpose.RESET_PASSWORD);
        User user=token.getUser();
        if(invitation){requireLocalActive(user);if(user.hasPassword() || user.isEmailVerified())throw invalidToken();user.setEmailVerified(true);invalidateTokens(user.getId(),AccountTokenPurpose.INVITE_ACCOUNT);}
        else requirePasswordEligible(user);
        replacePassword(user, request.newPassword());
    }

    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        validateNewPassword(request.newPassword(), request.confirmPassword());
        User user = lockUser(userId);
        requirePasswordEligible(user);
        if (!user.hasPassword()
                || !passwords.matches(request.currentPassword(), user.getPassword())) {
            throw new AccountException(HttpStatus.BAD_REQUEST, "Your current password is incorrect.");
        }
        if (passwords.matches(request.newPassword(), user.getPassword())) {
            throw new AccountException(HttpStatus.BAD_REQUEST, "Your new password must be different from your current password.");
        }
        replacePassword(user, request.newPassword());
    }

    @Transactional(readOnly = true)
    public ProfileResponse profile(Long userId) {
        User user = users.findById(userId).orElseThrow(() ->
                new AccountException(HttpStatus.UNAUTHORIZED, "Please sign in again."));
        return ProfileResponse.from(user, identities.existsByUser_IdAndProvider(userId, AuthProvider.GOOGLE));
    }

    @Transactional
    @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('PROFILE_UPDATE') or hasAuthority('USER_ACCESS_UPDATE')")
    public ProfileResponse updateProfile(Long userId, ProfileUpdateRequest request) {
        User user = lockUser(userId);
        String name = request.fullName().trim();
        if (name.length() < 2) {
            throw new AccountException(HttpStatus.BAD_REQUEST, "Full name must contain at least 2 characters.");
        }
        // Explicit allowlist: email, password, role, provider and status cannot be changed here.
        user.setFullName(name);
        user.setPhone(trimNullable(request.phone()));
        user.setAddress(trimNullable(request.address()));
        user.setAvatarUrl(trimNullable(request.avatarUrl()));
        return ProfileResponse.from(user, identities.existsByUser_IdAndProvider(userId, AuthProvider.GOOGLE));
    }

    private void replacePassword(User user, String newPassword) {
        user.setPassword(passwords.encode(newPassword));
        user.setAuthVersion(user.getAuthVersion() + 1);
        invalidateTokens(user.getId(), AccountTokenPurpose.RESET_PASSWORD);
    }

    @Transactional
    public void invite(User user,String message) {
        if(user.hasPassword() || user.isEmailVerified())throw new IllegalArgumentException("Only pending accounts can be invited.");
        try { issueToken(user,AccountTokenPurpose.INVITE_ACCOUNT,message); }
        catch(MailException ex){throw new AccountException(HttpStatus.SERVICE_UNAVAILABLE,"Unable to send the invitation. Please try again.");}
    }
    @Transactional
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN') and hasAuthority('USER_ACCESS_UPDATE')")
    public void requestAdminReset(Long id){
        User user=lockUser(id);requirePasswordEligible(user);
        try{issueToken(user,AccountTokenPurpose.RESET_PASSWORD);}catch(MailException ex){
            throw new AccountException(HttpStatus.SERVICE_UNAVAILABLE,"Unable to send the reset email. Please try again.");
        }
    }
    private void issueToken(User user,AccountTokenPurpose purpose){issueToken(user,purpose,null);}
    private void issueToken(User user, AccountTokenPurpose purpose,String personalMessage) {
        Instant now = clock.instant();
        boolean recentlySent = tokens.findFirstByUser_IdAndPurposeOrderByIssuedAtDesc(user.getId(), purpose)
                .map(token -> token.getIssuedAt().plusSeconds(60).isAfter(now)).orElse(false);
        if (recentlySent) return;
        invalidateTokens(user.getId(), purpose);
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        Duration lifetime = purpose != AccountTokenPurpose.RESET_PASSWORD
                ? AccountPolicy.VERIFICATION_TOKEN_LIFETIME : AccountPolicy.RESET_TOKEN_LIFETIME;
        tokens.save(AccountToken.builder().user(user).purpose(purpose).tokenHash(hash(rawToken))
                .issuedAt(now).expiresAt(now.plus(lifetime)).build());
        if(purpose==AccountTokenPurpose.INVITE_ACCOUNT)mail.sendInvitation(user.getEmail(),rawToken,personalMessage);
        else mail.send(user.getEmail(), rawToken, purpose);
    }

    private AccountToken lockValidToken(String rawToken, AccountTokenPurpose purpose) {
        String tokenHash = hash(rawToken);
        Long userId = tokens.findUserIdByHash(tokenHash).orElseThrow(AccountServiceImpl::invalidToken);
        // All issuance, consumption and password changes lock the user first, preventing token replay races.
        lockUser(userId);
        // Locking reads also see the latest committed state on MySQL REPEATABLE READ.
        AccountToken token = tokens.findLockedByHash(tokenHash).orElseThrow(AccountServiceImpl::invalidToken);
        validateToken(token, purpose);
        return token;
    }

    private void validateToken(AccountToken token, AccountTokenPurpose purpose) {
        if (token.getPurpose() != purpose || token.getUsedAt() != null
                || !token.getExpiresAt().isAfter(clock.instant())) throw invalidToken();
    }

    private User lockUser(Long id) {
        return users.findLockedById(id).orElseThrow(() ->
                new AccountException(HttpStatus.UNAUTHORIZED, "Please sign in again."));
    }

    private void requireLocalActive(User user) {
        if (user.getProvider() != AuthProvider.LOCAL || !user.isEnabled() || user.getStatus() != UserStatus.ACTIVE) {
            throw new AccountException(HttpStatus.BAD_REQUEST, "This account is not eligible for this action.");
        }
    }

    private void requirePasswordEligible(User user) {
        if (!user.isEnabled() || !user.isEmailVerified() || user.getStatus() != UserStatus.ACTIVE) {
            throw new AccountException(HttpStatus.BAD_REQUEST, "This account is not eligible for this action.");
        }
    }

    private void invalidateTokens(Long userId, AccountTokenPurpose purpose) {
        tokens.findByUser_IdAndPurposeAndUsedAtIsNull(userId, purpose)
                .forEach(token -> token.setUsedAt(clock.instant()));
    }

    private static String hash(String rawToken) {
        if (rawToken == null || rawToken.isBlank() || rawToken.length() > 200) throw invalidToken();
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(rawToken.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 unavailable", ex);
        }
    }

    private static AccountException invalidToken() {
        return new AccountException(HttpStatus.BAD_REQUEST, "This link is invalid, has expired, or has already been used.");
    }

    private static String trimNullable(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
