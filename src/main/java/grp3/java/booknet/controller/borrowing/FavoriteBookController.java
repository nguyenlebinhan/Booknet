package grp3.java.booknet.controller.borrowing;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import grp3.java.booknet.security.principal.AccountPrincipal;
import grp3.java.booknet.service.borrowing.FavoriteBookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequestMapping("/member/favorites")
@RequiredArgsConstructor
public class FavoriteBookController {

    private final FavoriteBookService favoriteBookService;

    @PostMapping("/{bookId}/toggle")
    @PreAuthorize("hasAnyRole('MEMBER', 'USER')")
    public String toggleFavorite(
            Authentication authentication,
            @PathVariable Long bookId,
            @RequestParam(value = "redirectUrl", required = false) String redirectUrl,
            RedirectAttributes redirectAttributes) {

        Long userId = AccountPrincipal.id(authentication);
        boolean saved = favoriteBookService.toggleFavorite(userId, bookId);

        if (saved) {
            redirectAttributes.addFlashAttribute("successMessage", "Book saved to your favorite list in My Books.");
        } else {
            redirectAttributes.addFlashAttribute("successMessage", "Book removed from your favorite list.");
        }

        if (redirectUrl != null && !redirectUrl.isBlank() && redirectUrl.startsWith("/")) {
            return "redirect:" + redirectUrl;
        }
        return "redirect:/catalog/" + bookId;
    }

    @PostMapping("/{bookId}/remove")
    @PreAuthorize("hasAnyRole('MEMBER', 'USER')")
    public String removeFavorite(
            Authentication authentication,
            @PathVariable Long bookId,
            @RequestParam(value = "redirectUrl", required = false) String redirectUrl,
            RedirectAttributes redirectAttributes) {

        Long userId = AccountPrincipal.id(authentication);
        favoriteBookService.removeFavorite(userId, bookId);
        redirectAttributes.addFlashAttribute("successMessage", "Book removed from your favorite list.");

        if (redirectUrl != null && !redirectUrl.isBlank() && redirectUrl.startsWith("/")) {
            return "redirect:" + redirectUrl;
        }
        return "redirect:/member/my-books?tab=favorites";
    }

    @PostMapping("/{bookId}/toggle-ajax")
    @ResponseBody
    @PreAuthorize("hasAnyRole('MEMBER', 'USER')")
    public ResponseEntity<Map<String, Object>> toggleFavoriteAjax(
            Authentication authentication,
            @PathVariable Long bookId) {

        Long userId = AccountPrincipal.id(authentication);
        boolean saved = favoriteBookService.toggleFavorite(userId, bookId);
        return ResponseEntity.ok(Map.of("saved", saved, "bookId", bookId));
    }
}
