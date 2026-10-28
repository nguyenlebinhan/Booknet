package grp3.java.booknet.security;

import grp3.java.booknet.entity.user.OAuthIdentity;
import grp3.java.booknet.entity.user.User;
import grp3.java.booknet.enums.user.AuthProvider;
import grp3.java.booknet.enums.user.UserStatus;
import grp3.java.booknet.repository.user.OAuthIdentityRepository;
import grp3.java.booknet.repository.user.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
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
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;
    private final OAuthIdentityRepository oauthIdentityRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");
        String picture = oAuth2User.getAttribute("picture");
        String googleSub = oAuth2User.getAttribute("sub");
        Boolean emailVerified = oAuth2User.getAttribute("email_verified");

        if (email == null || email.isBlank() || googleSub == null || googleSub.isBlank()) {
            throw new OAuth2AuthenticationException("Google did not provide a valid email and subject.");
        }
        if (!Boolean.TRUE.equals(emailVerified)) {
            throw new OAuth2AuthenticationException("Google email must be verified before it can be linked.");
        }

        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        Long linkTargetUserId = getPendingLinkUserId();
        User user = linkTargetUserId == null
                ? loadOrCreateGoogleLogin(normalizedEmail, name, picture, googleSub)
                : linkGoogleToLocalUser(linkTargetUserId, normalizedEmail, googleSub);

        log.info("Google OAuth2 authentication completed for user id {}", user.getId());
        return new CustomOAuth2User(oAuth2User, user);
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
        if (authentication == null
                || !(authentication.getPrincipal() instanceof CustomUserDetails principal)
                || !userId.equals(principal.getId())
                || principal.getProvider() != AuthProvider.LOCAL) {
            throw new OAuth2AuthenticationException("Sign in with the local account before linking Google.");
        }
        return userId;
    }

    private User linkGoogleToLocalUser(Long userId, String email, String googleSub) {
        User localUser = userRepository.findById(userId)
                .orElseThrow(() -> new OAuth2AuthenticationException("The local account could not be found."));
        if (localUser.getProvider() != AuthProvider.LOCAL || !localUser.isEnabled()
                || localUser.getStatus() != UserStatus.ACTIVE) {
            throw new OAuth2AuthenticationException("Only an active local account can be linked to Google.");
        }

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

        oauthIdentityRepository.save(OAuthIdentity.builder()
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
            return identity.get().getUser();
        }

        Optional<User> existingUser = userRepository.findByEmail(email);
        User user;
        if (existingUser.isPresent()) {
            user = existingUser.get();
            if (user.getProvider() == AuthProvider.LOCAL) {
                throw new OAuth2AuthenticationException(
                        "A local account already uses this email. Sign in locally and link Google from Home.");
            }
            if (oauthIdentityRepository.existsByUser_IdAndProvider(user.getId(), AuthProvider.GOOGLE)) {
                throw new OAuth2AuthenticationException("A different Google account already uses this BookNet account.");
            }
        } else {
            String defaultUsername = email.substring(0, email.indexOf('@'));
            user = userRepository.save(User.builder()
                    .email(email)
                    .fullName(name != null && !name.isBlank() ? name : "Google User")
                    .username(defaultUsername)
                    .avatarUrl(picture)
                    .provider(AuthProvider.GOOGLE)
                    .role("ROLE_USER")
                    .enabled(true)
                    .emailVerified(true)
                    .status(UserStatus.ACTIVE)
                    .password(null)
                    .build());
        }

        oauthIdentityRepository.save(OAuthIdentity.builder()
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

    private HttpServletRequest currentRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
        return attributes.getRequest();
    }
}
