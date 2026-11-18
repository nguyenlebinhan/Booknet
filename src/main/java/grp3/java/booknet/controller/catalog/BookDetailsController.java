package grp3.java.booknet.controller.catalog;

import grp3.java.booknet.service.catalog.BookDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/** Public Book details & shelf location (UC08). Open to guests; staff also see unpublished books. */
@Controller
@RequiredArgsConstructor
public class BookDetailsController {
    private final BookDetailsService details;

    @GetMapping("/catalog/{id:\\d+}")
    public String details(@PathVariable Long id, Authentication authentication, Model model) {
        model.addAttribute("book", details.details(id, isStaff(authentication)));
        model.addAttribute("activePage", "catalog");
        return "catalog/details";
    }

    private static boolean isStaff(Authentication authentication) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_LIBRARIAN".equals(a.getAuthority()) || "ROLE_ADMIN".equals(a.getAuthority()));
    }
}
