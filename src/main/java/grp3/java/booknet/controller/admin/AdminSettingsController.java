package grp3.java.booknet.controller.admin;

import grp3.java.booknet.dto.admin.CirculationPolicyForm;
import grp3.java.booknet.entity.book_service.CirculationPolicy;
import grp3.java.booknet.security.principal.CustomOAuth2User;
import grp3.java.booknet.security.dto.CustomUserDetails;
import grp3.java.booknet.service.admin.AuditLogService;
import grp3.java.booknet.service.admin.CirculationPolicyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Controller cho cấu hình chính sách lưu thông thư viện (UC44) — /admin/settings.
 * Role: ADMIN ONLY.
 */
@Slf4j
@Controller
@RequestMapping("/admin/settings")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminSettingsController {

    private final CirculationPolicyService policyService;
    private final AuditLogService auditLogService;

    @GetMapping
    public String showSettings(Authentication authentication, Model model) {
        CirculationPolicy policy = policyService.getPolicy();

        CirculationPolicyForm form = CirculationPolicyForm.builder()
                .maxLoansPerMember(policy.getMaxLoansPerMember())
                .loanPeriodDays(policy.getLoanPeriodDays())
                .renewPeriodDays(policy.getRenewPeriodDays())
                .dailyFineRate(policy.getDailyFineRate())
                .reservationHoldDays(policy.getReservationHoldDays())
                .build();

        model.addAttribute("policyForm", form);
        model.addAttribute("policy", policy);
        model.addAttribute("adminName", displayName(authentication));
        model.addAttribute("activePage", "settings");

        return "admin/settings/policies";
    }

    @PostMapping
    public String updateSettings(
            @Valid @ModelAttribute("policyForm") CirculationPolicyForm form,
            BindingResult bindingResult,
            Authentication authentication,
            RedirectAttributes redirectAttributes,
            Model model
    ) {
        CirculationPolicy current = policyService.getPolicy();

        if (bindingResult.hasErrors()) {
            model.addAttribute("policy", current);
            model.addAttribute("adminName", displayName(authentication));
            model.addAttribute("activePage", "settings");
            return "admin/settings/policies";
        }

        String adminEmail = authentication.getName();

        CirculationPolicy updated = CirculationPolicy.builder()
                .maxLoansPerMember(form.getMaxLoansPerMember())
                .loanPeriodDays(form.getLoanPeriodDays())
                .renewPeriodDays(form.getRenewPeriodDays())
                .dailyFineRate(form.getDailyFineRate())
                .reservationHoldDays(form.getReservationHoldDays())
                .build();

        policyService.updatePolicy(updated, adminEmail);

        // Ghi Audit Log (UC43)
        auditLogService.log(
                adminEmail,
                "ADMIN",
                "POLICY_UPDATED",
                "CirculationPolicy",
                1L,
                "Updated circulation policies: quota=" + form.getMaxLoansPerMember() +
                ", loanDays=" + form.getLoanPeriodDays() +
                ", renewDays=" + form.getRenewPeriodDays() +
                ", dailyFineRate=" + form.getDailyFineRate() + " ₫/day"
        );

        redirectAttributes.addFlashAttribute("successMessage",
                "Circulation policies have been updated successfully.");

        return "redirect:/admin/settings";
    }

    private static String displayName(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        if (principal instanceof CustomUserDetails u) return u.getFullName();
        if (principal instanceof CustomOAuth2User u) return u.getFullName();
        return authentication.getName();
    }
}
