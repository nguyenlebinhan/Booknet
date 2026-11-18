package grp3.java.booknet.controller.notification;

import grp3.java.booknet.dto.notification.MemberNotificationsView;
import grp3.java.booknet.dto.notification.NotificationPreferenceForm;
import grp3.java.booknet.exception.account.AccountException;
import grp3.java.booknet.security.principal.AccountPrincipal;
import grp3.java.booknet.service.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/member/notifications")
@PreAuthorize("hasRole('MEMBER')")
@RequiredArgsConstructor
public class MemberNotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public String showNotifications(
            Authentication authentication,
            @RequestParam(defaultValue = "all") String filter,
            Model model
    ) {
        Long userId = AccountPrincipal.id(authentication);
        MemberNotificationsView view = notificationService.getMemberNotificationsView(userId, filter);

        model.addAttribute("view", view);
        if (!model.containsAttribute("preferenceForm")) {
            model.addAttribute("preferenceForm", view.preferences());
        }
        model.addAttribute("activePage", "notifications");
        return "notification/notifications";
    }

    @PostMapping("/{id}/read")
    public String markAsRead(
            Authentication authentication,
            @PathVariable Long id,
            @RequestParam(defaultValue = "all") String filter,
            RedirectAttributes redirectAttributes
    ) {
        Long userId = AccountPrincipal.id(authentication);
        try {
            notificationService.markAsRead(userId, id);
        } catch (AccountException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/member/notifications?filter=" + sanitizeFilter(filter);
    }

    @PostMapping("/read-all")
    public String markAllAsRead(
            Authentication authentication,
            @RequestParam(defaultValue = "all") String filter,
            RedirectAttributes redirectAttributes
    ) {
        Long userId = AccountPrincipal.id(authentication);
        int updated = notificationService.markAllAsRead(userId);
        if (updated > 0) {
            redirectAttributes.addFlashAttribute("successMessage",
                    "Marked " + updated + " notification(s) as read.");
        } else {
            redirectAttributes.addFlashAttribute("successMessage", "All notifications are already read.");
        }
        return "redirect:/member/notifications?filter=" + sanitizeFilter(filter);
    }

    @PostMapping("/preferences")
    public String updatePreferences(
            Authentication authentication,
            @ModelAttribute("preferenceForm") NotificationPreferenceForm form,
            @RequestParam(defaultValue = "all") String filter,
            RedirectAttributes redirectAttributes
    ) {
        Long userId = AccountPrincipal.id(authentication);
        try {
            notificationService.updatePreferences(userId, form);
            redirectAttributes.addFlashAttribute("successMessage", "Your reminder preferences have been saved.");
        } catch (AccountException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/member/notifications?filter=" + sanitizeFilter(filter);
    }

    private String sanitizeFilter(String filter) {
        if (filter == null) {
            return "all";
        }
        return switch (filter.trim().toLowerCase()) {
            case "unread", "loans", "reservations", "all" -> filter.trim().toLowerCase();
            default -> "all";
        };
    }
}
