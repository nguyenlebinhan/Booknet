package grp3.java.booknet.controller.staff;

import grp3.java.booknet.entity.book_service.Fine;
import grp3.java.booknet.entity.user.User;
import grp3.java.booknet.enums.book_service.FineStatus;
import grp3.java.booknet.exception.account.AccountException;
import grp3.java.booknet.repository.user.UserRepository;
import grp3.java.booknet.security.principal.AccountPrincipal;
import grp3.java.booknet.security.principal.CustomOAuth2User;
import grp3.java.booknet.security.dto.CustomUserDetails;
import grp3.java.booknet.service.admin.AuditLogService;
import grp3.java.booknet.service.fine.FineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.List;

/**
 * Controller cho chức năng Quản lý & Thu phí quá hạn (UC29) — /staff/fines.
 * Dành cho LIBRARIAN và ADMIN.
 */
@Slf4j
@Controller
@RequestMapping("/staff/fines")
@PreAuthorize("hasAnyRole('LIBRARIAN', 'ADMIN')")
@RequiredArgsConstructor
public class StaffFinesController {

    private final FineService fineService;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    @GetMapping
    public String listFines(
            @RequestParam(value = "status", required = false) FineStatus status,
            @RequestParam(value = "keyword", required = false) String keyword,
            Authentication authentication,
            Model model
    ) {
        List<Fine> fines = fineService.searchFines(status, keyword);

        long unpaidCount = fines.stream().filter(f -> f.getStatus() == FineStatus.UNPAID).count();
        long paidCount = fines.stream().filter(f -> f.getStatus() == FineStatus.PAID).count();
        BigDecimal unpaidAmount = fines.stream()
                .filter(f -> f.getStatus() == FineStatus.UNPAID)
                .map(Fine::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        model.addAttribute("fines", fines);
        model.addAttribute("currentStatus", status);
        model.addAttribute("keyword", keyword);
        model.addAttribute("unpaidCount", unpaidCount);
        model.addAttribute("paidCount", paidCount);
        model.addAttribute("unpaidAmount", unpaidAmount);
        model.addAttribute("adminName", displayName(authentication));
        model.addAttribute("activePage", "fines");

        return "staff/fines/list";
    }

    @PostMapping("/{id}/collect")
    public String collectFineAtDesk(
            @PathVariable("id") Long fineId,
            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {
        Long staffId = AccountPrincipal.id(authentication);
        User staff = userRepository.findById(staffId)
                .orElseThrow(() -> new AccountException(HttpStatus.UNAUTHORIZED, "Staff account not found."));

        String transactionRef = "DESK-" + System.currentTimeMillis();
        Fine collected = fineService.confirmPaymentAtDesk(fineId, staff, transactionRef);

        // Ghi Audit Log (UC43)
        auditLogService.log(
                staff.getEmail(),
                staff.getRole() != null ? staff.getRole().name() : "LIBRARIAN",
                "FINE_CONFIRMED",
                "Fine",
                collected.getId(),
                "Collected fine " + collected.getFineCode() + " (" + collected.getAmount() + " ₫) at desk from "
                        + (collected.getMember() != null ? collected.getMember().getEmail() : "member")
        );

        redirectAttributes.addFlashAttribute("successMessage",
                "Successfully confirmed payment for " + collected.getFineCode() + " (" + collected.getAmount() + " ₫).");

        return "redirect:/staff/fines";
    }

    private static String displayName(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        if (principal instanceof CustomUserDetails u) return u.getFullName();
        if (principal instanceof CustomOAuth2User u) return u.getFullName();
        return authentication.getName();
    }
}
