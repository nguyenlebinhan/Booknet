package grp3.java.booknet.service;

import grp3.java.booknet.entity.user.OAuthIdentity;
import grp3.java.booknet.entity.user.User;
import grp3.java.booknet.enums.user.AuthProvider;
import grp3.java.booknet.enums.user.UserStatus;
import grp3.java.booknet.enums.user.UserRole;
import grp3.java.booknet.repository.user.OAuthIdentityRepository;
import grp3.java.booknet.repository.user.UserRepository;
import grp3.java.booknet.security.CustomUserDetails;
import grp3.java.booknet.security.OAuthLinkSessionAttributes;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Locale;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OAuthAccountService {

    private final UserRepository userRepository;
    private final OAuthIdentityRepository oauthIdentityRepository;

    @Transactional
    public User authenticate(OAuth2User oAuth2User) throws OAuth2AuthenticationException {
        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");
        String picture = oAuth2User.getAttribute("picture");
        String googleSub = oAuth2User.getAttribute("sub");
        Boolean emailVerified = oAuth2User.getAttribute("email_verified");

        if (email == null || email.length() > 120 || !email.matches("[^@\\s]+@[^@\\s]+")
                || googleSub == null || googleSub.isBlank() || googleSub.length() > 200) {
            throw new OAuth2AuthenticationException("Google did not provide a valid email and subject.");
        }
        if (!Boolean.TRUE.equals(emailVerified)) {
            throw new OAuth2AuthenticationException("Google email must be verified before it can be linked.");
        }

        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        Long linkTargetUserId = getPendingLinkUserId();
        try {
            User user = linkTargetUserId == null
                    ? loadOrCreateGoogleLogin(normalizedEmail, name, picture, googleSub)
                    : linkGoogleToLocalUser(linkTargetUserId, normalizedEmail, googleSub);
            requireActive(user);
            return user;
        } catch (DataIntegrityViolationException ex) {
            throw new OAuth2AuthenticationException("Google identity was changed concurrently. Please try again.");
        }
    }

    private Long getPendingLinkUserId() {
        HttpServletRequest request = currentRequest();
        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }

        Object rawUserId = session.getAttribute(OAuthLinkSessionAttributes.GOOGLE_LINK_USER_ID);
        if (rawUserId == null) {
            return null;
        }

        Object rawStartedAt = session.getAttribute(OAuthLinkSessionAttributes.GOOGLE_LINK_STARTED_AT);
        if (!(rawUserId instanceof Long userId)
                || !(rawStartedAt instanceof Long startedAt)
                || System.currentTimeMillis() - startedAt > OAuthLinkSessionAttributes.GOOGLE_LINK_MAX_AGE_MILLIS) {
            throw new OAuth2AuthenticationException("Google account linking request expired. Please try again.");
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof UsernamePasswordAuthenticationToken)
                || !(authentication.getPrincipal() instanceof CustomUserDetails principal)
                || !userId.equals(principal.getId())
                || principal.getProvider() != AuthProvider.LOCAL) {
            throw new OAuth2AuthenticationException("Sign in with the local account before linking Google.");
        }
        return userId;
    }

    private User linkGoogleToLocalUser(Long userId, String email, String googleSub) {
        User localUser = userRepository.findLockedById(userId)
                .orElseThrow(() -> new OAuth2AuthenticationException("The local account could not be found."));
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (localUser.getProvider() != AuthProvider.LOCAL || !localUser.isEmailVerified()
                || !(authentication.getPrincipal() instanceof CustomUserDetails principal)
                || principal.getAuthVersion() != localUser.getAuthVersion()) {
            throw new OAuth2AuthenticationException("Only an active local account can be linked to Google.");
        }
        requireActive(localUser);

        Optional<OAuthIdentity> identityBySubject = oauthIdentityRepository
                .findByProviderAndProviderUserId(AuthProvider.GOOGLE, googleSub);
        if (identityBySubject.isPresent()) {
            if (!identityBySubject.get().getUser().getId().equals(localUser.getId())) {
                throw new OAuth2AuthenticationException("This Google account is already linked to another BookNet account.");
            }
            markLinkSucceeded();
            return localUser;
        }

        if (oauthIdentityRepository.existsByUser_IdAndProvider(localUser.getId(), AuthProvider.GOOGLE)) {
            throw new OAuth2AuthenticationException("A different Google account is already linked to this BookNet account.");
        }

        oauthIdentityRepository.saveAndFlush(OAuthIdentity.builder()
                .user(localUser)
                .provider(AuthProvider.GOOGLE)
                .providerUserId(googleSub)
                .providerSubject(googleSub)
                .email(email)
                .build());
        markLinkSucceeded();
        return localUser;
    }

    private User loadOrCreateGoogleLogin(String email, String name, String picture, String googleSub) {
        Optional<OAuthIdentity> identity = oauthIdentityRepository
                .findByProviderAndProviderUserId(AuthProvider.GOOGLE, googleSub);
        if (identity.isPresent()) {
            return userRepository.findLockedById(identity.get().getUser().getId())
                    .orElseThrow(() -> new OAuth2AuthenticationException("BookNet account not found."));
        }

        Optional<Long> existingUserId = userRepository.findIdByEmail(email);
        User user;
        if (existingUserId.isPresent()) {
            user = userRepository.findLockedById(existingUserId.get()).orElseThrow();
            if (user.getProvider() == AuthProvider.LOCAL) {
                throw new OAuth2AuthenticationException(
                        "A local account already uses this email. Sign in locally and link Google from Home.");
            }
            if (oauthIdentityRepository.existsByUser_IdAndProvider(user.getId(), AuthProvider.GOOGLE)) {
                throw new OAuth2AuthenticationException("A different Google account already uses this BookNet account.");
            }
        } else {
            String defaultUsername = email.substring(0, email.indexOf('@'));
            user = userRepository.saveAndFlush(User.builder()
                    .email(email)
                    .fullName(truncate(name != null && !name.isBlank() ? name : "Google User", 120))
                    .username(truncate(defaultUsername, 50))
                    .avatarUrl(truncate(picture, 500))
                    .provider(AuthProvider.GOOGLE)
                    .role(UserRole.ROLE_USER)
                    .enabled(true)
                    .emailVerified(true)
                    .status(UserStatus.ACTIVE)
                    .password(null)
                    .build());
        }

        requireActive(user);
        oauthIdentityRepository.saveAndFlush(OAuthIdentity.builder()
                .user(user)
                .provider(AuthProvider.GOOGLE)
                .providerUserId(googleSub)
                .providerSubject(googleSub)
                .email(email)
                .build());
        return user;
    }

    private void markLinkSucceeded() {
        HttpSession session = currentRequest().getSession(false);
        if (session != null) {
            session.setAttribute(OAuthLinkSessionAttributes.GOOGLE_LINK_SUCCEEDED, Boolean.TRUE);
        }
    }

    private void requireActive(User user) {
        if (!user.isEnabled() || !user.isEmailVerified() || user.getStatus() != UserStatus.ACTIVE) {
            throw new OAuth2AuthenticationException("The BookNet account is disabled, unverified, suspended or expired.");
        }
    }

    private static String truncate(String value, int max) {
        return value == null ? null : value.substring(0, Math.min(max, value.length()));
    }

    private HttpServletRequest currentRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
        return attributes.getRequest();
    }
}
