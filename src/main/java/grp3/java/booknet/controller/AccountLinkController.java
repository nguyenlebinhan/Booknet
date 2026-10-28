package grp3.java.booknet.controller;

import grp3.java.booknet.enums.user.AuthProvider;
import grp3.java.booknet.repository.user.OAuthIdentityRepository;
import grp3.java.booknet.security.CustomUserDetails;
import grp3.java.booknet.security.OAuthLinkSessionAttributes;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class AccountLinkController {

    private final OAuthIdentityRepository oauthIdentityRepository;

    @PostMapping("/account/link/google")
    public String linkGoogle(
            @AuthenticationPrincipal CustomUserDetails user,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        if (user == null || user.getProvider() != AuthProvider.LOCAL) {
            redirectAttributes.addFlashAttribute("googleLinkError",
                    "Hãy đăng nhập bằng email và mật khẩu trước khi liên kết Google.");
            return "redirect:/";
        }

        if (oauthIdentityRepository.existsByUser_IdAndProvider(user.getId(), AuthProvider.GOOGLE)) {
            redirectAttributes.addFlashAttribute("googleLinkInfo", "Tài khoản này đã được liên kết Google.");
            return "redirect:/";
        }

        session.setAttribute(OAuthLinkSessionAttributes.GOOGLE_LINK_USER_ID, user.getId());
        session.setAttribute(OAuthLinkSessionAttributes.GOOGLE_LINK_STARTED_AT, System.currentTimeMillis());
        session.removeAttribute(OAuthLinkSessionAttributes.GOOGLE_LINK_SUCCEEDED);
        return "redirect:/oauth2/authorization/google";
    }
}
