package grp3.java.booknet.controller;

import grp3.java.booknet.service.MemberDirectoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Set;

@Controller
@RequiredArgsConstructor
public class MemberManagementController {
    private final MemberDirectoryService directory;

    @GetMapping({"/staff/members", "/admin/users"})
    @PreAuthorize("hasAuthority('MEMBER_READ')")
    public String list(@RequestParam(defaultValue = "false") boolean demo,
                       @RequestParam(defaultValue = "") String keyword,
                       @RequestParam(defaultValue = "") String card,
                       @RequestParam(defaultValue = "") String loan,
                       @RequestParam(defaultValue = "") String fine,
                       @RequestParam(defaultValue = "0") int page,
                       jakarta.servlet.http.HttpServletRequest request, Model model) {
        boolean admin = request.getServletPath().startsWith("/admin/");
        // requestURI also supports MockMvc's default empty servletPath and context-path deployments.
        admin = admin || request.getRequestURI().startsWith(request.getContextPath() + "/admin/");
        if (keyword.length() > 120 || !Set.of("", "Active", "Expired", "Pending").contains(card)
                || !Set.of("", "borrowed", "overdue", "none").contains(loan)
                || !Set.of("", "unpaid", "clear").contains(fine)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
        model.addAttribute("directory", directory.directory(admin, demo, keyword.trim(), card, loan, fine, page));
        common(model, admin, demo);
        model.addAttribute("keyword", keyword.trim());
        model.addAttribute("card", card);
        model.addAttribute("loan", loan);
        model.addAttribute("fine", fine);
        return "member-management";
    }

    @GetMapping({"/staff/members/{id}", "/admin/users/{id}"})
    @PreAuthorize("hasAuthority('MEMBER_READ')")
    public String detail(@PathVariable Long id, @RequestParam(defaultValue = "false") boolean demo,
                         jakarta.servlet.http.HttpServletRequest request, Model model) {
        boolean admin = request.getRequestURI().startsWith(request.getContextPath() + "/admin/");
        model.addAttribute("member", directory.detail(admin, demo, id));
        model.addAttribute("loans", directory.previewLoans(demo, id));
        common(model, admin, demo);
        return "member-details";
    }

    private void common(Model model, boolean admin, boolean demo) {
        model.addAttribute("adminArea", admin);
        model.addAttribute("demo", demo);
        model.addAttribute("basePath", admin ? "/admin/users" : "/staff/members");
        model.addAttribute("activePage", admin ? "users" : "members");
    }
}
