package grp3.java.booknet.controller.notification;

import grp3.java.booknet.repository.user.UserRepository;
import grp3.java.booknet.security.dto.CustomUserDetails;
import grp3.java.booknet.security.principal.CustomOAuth2User;
import grp3.java.booknet.service.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
@RequiredArgsConstructor
public class NotificationHeaderAdvice {

    private final NotificationService notificationService;
    private final UserRepository userRepository;

    @ModelAttribute("unreadNotificationCount")
    public long unreadNotificationCount(Authentication authentication) {
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return 0L;
        }
        boolean isMember = authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_MEMBER".equals(a.getAuthority()) || "ROLE_USER".equals(a.getAuthority()));
        if (!isMember) {
            return 0L;
        }

        Long userId = null;
        Object principal = authentication.getPrincipal();
        if (principal instanceof CustomUserDetails local) {
            userId = local.getId();
        } else if (principal instanceof CustomOAuth2User oauth) {
            userId = oauth.getId();
        } else if (authentication.getName() != null) {
            userId = userRepository.findIdByEmail(authentication.getName()).orElse(null);
        }

        if (userId == null) {
            return 0L;
        }
        return notificationService.countUnread(userId);
    }
}
