package grp3.java.booknet.controller.admin;

import grp3.java.booknet.entity.AuditLog;
import grp3.java.booknet.security.principal.CustomOAuth2User;
import grp3.java.booknet.security.dto.CustomUserDetails;
import grp3.java.booknet.service.admin.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Controller cho chức năng xem System Audit Log (UC43) — /admin/audit-logs.
 * Role: ADMIN ONLY.
 */
@Controller
@RequestMapping("/admin/audit-logs")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminAuditLogController {

    private final AuditLogService auditLogService;

    @GetMapping
    public String listAuditLogs(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "action", required = false) String action,
            @RequestParam(value = "page", defaultValue = "0") int page,
            Authentication authentication,
            Model model
    ) {
        int pageSize = 15;
        Page<AuditLog> logPage = auditLogService.searchWithKeyword(
                keyword, action, PageRequest.of(Math.max(0, page), pageSize));

        model.addAttribute("logs", logPage);
        model.addAttribute("keyword", keyword);
        model.addAttribute("currentAction", action);
        model.addAttribute("adminName", displayName(authentication));
        model.addAttribute("activePage", "audit-logs");

        return "admin/audit-logs/list";
    }

    private static String displayName(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        if (principal instanceof CustomUserDetails u) return u.getFullName();
        if (principal instanceof CustomOAuth2User u) return u.getFullName();
        return authentication.getName();
    }
}
