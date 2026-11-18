package grp3.java.booknet.controller.reservation;

import grp3.java.booknet.dto.reservation.LibrarianReservationsView;
import grp3.java.booknet.entity.book_service.Loan;
import grp3.java.booknet.entity.book_service.Reservation;
import grp3.java.booknet.exception.reservation.ReservationException;
import grp3.java.booknet.service.reservation.ReservationQueueService;
import grp3.java.booknet.service.reservation.ReservationService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping({"/staff/reservations", "/librarian/reservations", "/admin/reservations"})
@PreAuthorize("hasAuthority('CIRCULATION')")
@RequiredArgsConstructor
public class LibrarianReservationController {

    private final ReservationService reservationService;
    private final ReservationQueueService reservationQueueService;

    @GetMapping
    public String listReservations(
            @RequestParam(defaultValue = "pending") String tab,
            @RequestParam(required = false) String keyword,
            HttpServletRequest request,
            Model model
    ) {
        LibrarianReservationsView view = reservationService.getLibrarianReservationsView(tab, keyword);
        model.addAttribute("view", view);
        model.addAttribute("basePath", resolveBasePath(request));
        model.addAttribute("activePage", "reservations");
        model.addAttribute("section", "Reservation management");
        return "reservation/manage";
    }

    @PostMapping("/{id}/assign-copy")
    public String assignCopy(
            @PathVariable Long id,
            Authentication authentication,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes
    ) {
        String basePath = resolveBasePath(request);
        try {
            Reservation ready = reservationService.assignCopyAndMarkReady(id, authentication.getName());
            redirectAttributes.addFlashAttribute("successMessage",
                    "Copy " + (ready.getAssignedCopy() != null ? ready.getAssignedCopy().getBarcode() : "")
                            + " held for reservation " + ready.displayCode() + " (" + ready.getMember().getFullName() + ").");
            return "redirect:" + basePath + "?tab=ready";
        } catch (ReservationException | IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:" + basePath + "?tab=pending";
        }
    }

    @PostMapping("/{id}/fulfill")
    public String fulfillReservation(
            @PathVariable Long id,
            Authentication authentication,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes
    ) {
        String basePath = resolveBasePath(request);
        try {
            Loan loan = reservationService.fulfillReservation(id, authentication.getName());
            redirectAttributes.addFlashAttribute("successMessage",
                    "Book handed over successfully. Loan #" + loan.getId()
                            + " created for " + loan.getMember().getFullName() + ".");
            return "redirect:" + basePath + "?tab=history";
        } catch (ReservationException | IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:" + basePath + "?tab=ready";
        }
    }

    @PostMapping("/{id}/cancel")
    public String cancelReservation(
            @PathVariable Long id,
            @RequestParam(required = false) String reason,
            @RequestParam(defaultValue = "pending") String tab,
            Authentication authentication,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes
    ) {
        String basePath = resolveBasePath(request);
        try {
            reservationService.cancelReservationByStaff(id, reason, authentication.getName());
            redirectAttributes.addFlashAttribute("successMessage",
                    "Reservation canceled and any held copy was advanced to the queue or released.");
        } catch (ReservationException | IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:" + basePath + "?tab=" + tab;
    }

    @PostMapping("/expire-holds")
    public String expireOverdueHolds(
            HttpServletRequest request,
            RedirectAttributes redirectAttributes
    ) {
        String basePath = resolveBasePath(request);
        int expired = reservationQueueService.expireOverdueReservations();
        if (expired > 0) {
            redirectAttributes.addFlashAttribute("successMessage",
                    "Processed " + expired + " expired pickup hold(s) and updated queues.");
        } else {
            redirectAttributes.addFlashAttribute("successMessage",
                    "No expired pickup holds found at this time.");
        }
        return "redirect:" + basePath + "?tab=ready";
    }

    private String resolveBasePath(HttpServletRequest request) {
        String uri = request.getRequestURI();
        if (uri != null && uri.startsWith("/admin/reservations")) {
            return "/admin/reservations";
        }
        if (uri != null && uri.startsWith("/librarian/reservations")) {
            return "/librarian/reservations";
        }
        return "/staff/reservations";
    }
}
