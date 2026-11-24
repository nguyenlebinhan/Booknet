package grp3.java.booknet.controller.reservation;

import grp3.java.booknet.dto.reservation.MemberReservationsView;
import grp3.java.booknet.entity.book_service.Reservation;
import grp3.java.booknet.exception.reservation.ReservationException;
import grp3.java.booknet.security.principal.AccountPrincipal;
import grp3.java.booknet.service.reservation.ReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/member/reservations")
@RequiredArgsConstructor
public class MemberReservationController {

    private final ReservationService reservationService;

    @GetMapping
    @PreAuthorize("hasRole('MEMBER')")
    public String myReservations(
            Authentication authentication,
            @RequestParam(defaultValue = "active") String tab,
            @RequestParam(required = false) Long cancelId,
            Model model
    ) {
        Long userId = AccountPrincipal.id(authentication);
        MemberReservationsView view = reservationService.getMemberReservationsView(userId, cancelId);
        boolean canReserve = authentication.getAuthorities().stream()
                .anyMatch(a -> "RESERVE".equals(a.getAuthority()));

        model.addAttribute("view", view);
        model.addAttribute("activeTab", normalizeTab(tab));
        model.addAttribute("activePage", "reservations");
        model.addAttribute("canReserve", canReserve);
        return "reservation/my-reservations";
    }

    @PostMapping
    @PreAuthorize("hasAuthority('RESERVE')")
    public String reserveBook(
            Authentication authentication,
            @RequestParam Long bookId,
            @RequestParam(required = false) String redirectTo,
            RedirectAttributes redirectAttributes
    ) {
        Long userId = AccountPrincipal.id(authentication);
        try {
            Reservation created = reservationService.reserveBook(userId, bookId);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Reservation " + created.displayCode() + " placed for \"" + created.getBook().getTitle()
                            + "\". You will be notified as soon as a copy is ready.");
        } catch (ReservationException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        if ("catalog".equalsIgnoreCase(redirectTo)) {
            return "redirect:/catalog";
        }
        return "redirect:/member/reservations";
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('RESERVE')")
    public String cancelReservation(
            Authentication authentication,
            @PathVariable Long id,
            RedirectAttributes redirectAttributes
    ) {
        Long userId = AccountPrincipal.id(authentication);
        try {
            reservationService.cancelMemberReservation(userId, id);
            redirectAttributes.addFlashAttribute("successMessage", "Your reservation has been canceled.");
        } catch (ReservationException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/member/reservations";
    }

    private String normalizeTab(String tab) {
        if (tab == null) {
            return "active";
        }
        return switch (tab.trim().toLowerCase()) {
            case "completed", "canceled", "active" -> tab.trim().toLowerCase();
            default -> "active";
        };
    }
}
