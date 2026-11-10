package grp3.java.booknet.controller;

import grp3.java.booknet.enums.user.AuthProvider;
import grp3.java.booknet.repository.user.OAuthIdentityRepository;
import grp3.java.booknet.security.CustomUserDetails;
import grp3.java.booknet.security.OAuthLinkSessionAttributes;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class AccountLinkController {

    private final OAuthIdentityRepository oauthIdentityRepository;

    @PostMapping("/account/link/google")
    public String linkGoogle(
            @AuthenticationPrincipal CustomUserDetails user,
            Authentication authentication,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        if (!(authentication instanceof UsernamePasswordAuthenticationToken)
                || user == null || user.getProvider() != AuthProvider.LOCAL || !user.isEnabled()) {
            redirectAttributes.addFlashAttribute("googleLinkError",
                    "Please sign in with your email and password before linking Google.");
            return "redirect:/";
        }

        if (oauthIdentityRepository.existsByUser_IdAndProvider(user.getId(), AuthProvider.GOOGLE)) {
            redirectAttributes.addFlashAttribute("googleLinkInfo", "This account is already linked to Google.");
            return "redirect:/";
        }

        session.setAttribute(OAuthLinkSessionAttributes.GOOGLE_LINK_USER_ID, user.getId());
        session.setAttribute(OAuthLinkSessionAttributes.GOOGLE_LINK_STARTED_AT, System.currentTimeMillis());
        session.removeAttribute(OAuthLinkSessionAttributes.GOOGLE_LINK_SUCCEEDED);
        return "redirect:/oauth2/authorization/google";
    }
}
