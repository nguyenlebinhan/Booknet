package grp3.java.booknet.controller;

import grp3.java.booknet.entity.user.User;
import grp3.java.booknet.enums.user.AuthProvider;
import grp3.java.booknet.repository.user.OAuthIdentityRepository;
import grp3.java.booknet.security.CustomOAuth2User;
import grp3.java.booknet.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final OAuthIdentityRepository oauthIdentityRepository;

    @GetMapping({"/", "/home"})
    public String home(
            @AuthenticationPrincipal Object principal,
            @RequestParam(value = "google_linked", required = false) String googleLinked,
            @RequestParam(value = "google_link_error", required = false) String googleLinkError,
            Model model) {
        User user = null;
        boolean canStartLink = false;

        if (principal instanceof CustomUserDetails localPrincipal) {
            canStartLink = localPrincipal.getProvider() == AuthProvider.LOCAL;
        } else if (principal instanceof CustomOAuth2User oauthPrincipal) {
            user = oauthPrincipal.getUser();
        }

        Long userId = principal instanceof CustomUserDetails localPrincipal
                ? localPrincipal.getId()
                : user != null ? user.getId() : null;
        boolean localAccount = principal instanceof CustomUserDetails localPrincipal
                ? localPrincipal.getProvider() == AuthProvider.LOCAL
                : user != null && user.getProvider() == AuthProvider.LOCAL;
        boolean linked = userId != null
                && oauthIdentityRepository.existsByUser_IdAndProvider(userId, AuthProvider.GOOGLE);

        model.addAttribute("showGoogleLink", canStartLink && localAccount && !linked);
        model.addAttribute("googleLinked", localAccount && linked && googleLinked == null);
        model.addAttribute("googleLinkedMessage", googleLinked != null);
        if (googleLinkError != null) {
            model.addAttribute("googleLinkError",
                    "Không thể liên kết Google. Tài khoản Google có thể đã thuộc tài khoản BookNet khác, hoặc thao tác đã hết hạn.");
        }
        return "home";
    }
}
