package grp3.java.booknet.controller.catalog;

import grp3.java.booknet.enums.book_service.ReservationStatus;
import grp3.java.booknet.repository.book_service.ReservationRepository;
import grp3.java.booknet.repository.user.UserRepository;
import grp3.java.booknet.security.dto.CustomUserDetails;
import grp3.java.booknet.security.principal.CustomOAuth2User;
import grp3.java.booknet.service.catalog.BookDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

/** Public Book details & shelf location (UC08). Open to guests; staff also see unpublished books. */
@Controller
@RequiredArgsConstructor
public class BookDetailsController {
    private final BookDetailsService details;
    private final ReservationRepository reservationRepository;
    private final UserRepository userRepository;

    @GetMapping("/catalog/{id:\\d+}")
    public String details(@PathVariable Long id, Authentication authentication, Model model) {
        model.addAttribute("book", details.details(id, isStaff(authentication)));
        model.addAttribute("alreadyReserved", isAlreadyReserved(id, authentication));
        model.addAttribute("activePage", "catalog");
        return "catalog/details";
    }

    private boolean isAlreadyReserved(Long bookId, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || isStaff(authentication)) {
            return false;
        }
        Long userId = null;
        if (authentication.getPrincipal() instanceof CustomUserDetails local) {
            userId = local.getId();
        } else if (authentication.getPrincipal() instanceof CustomOAuth2User oauth) {
            userId = oauth.getId();
        } else if (authentication.getName() != null) {
            userId = userRepository.findIdByEmail(authentication.getName()).orElse(null);
        }
        if (userId == null) {
            return false;
        }
        return reservationRepository.existsByMember_IdAndBook_IdAndStatusIn(
                userId, bookId, List.of(ReservationStatus.PENDING, ReservationStatus.READY));
    }

    private static boolean isStaff(Authentication authentication) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_LIBRARIAN".equals(a.getAuthority()) || "ROLE_ADMIN".equals(a.getAuthority()));
    }
}
