package grp3.java.booknet.controller.member;

import grp3.java.booknet.service.member.MemberDirectoryService;
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
    private final grp3.java.booknet.service.member.AccountAdministrationService administration;
    private final grp3.java.booknet.service.member.MemberCardService cards;
    private final grp3.java.booknet.repository.user.AccountAccessAuditRepository audits;
    private final grp3.java.booknet.service.impl.account.AccountServiceImpl accounts;

    @InitBinder({"accessForm", "statusForm", "activationForm"})
    void bind(org.springframework.web.bind.WebDataBinder binder) { binder.setDeclarativeBinding(true); }

    @ModelAttribute
    void rejectDemoWrites(jakarta.servlet.http.HttpServletRequest request) {
        if ("POST".equals(request.getMethod()) && "true".equalsIgnoreCase(request.getParameter("demo")))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Sample records cannot be changed.");
    }

    @GetMapping({"/staff/members", "/admin/users"})
    @PreAuthorize("hasAuthority('MEMBER_READ')")
    public String list(@RequestParam(defaultValue = "false") boolean demo,
                       @RequestParam(defaultValue = "") String keyword,
                       @RequestParam(defaultValue = "") String card,
                       @RequestParam(defaultValue = "") String loan,
                       @RequestParam(defaultValue = "") String fine,
                       @RequestParam(defaultValue = "0") int page,
                       @RequestParam(defaultValue = "") String status,
                       jakarta.servlet.http.HttpServletRequest request, Model model) {
        boolean admin = request.getServletPath().startsWith("/admin/");
        // requestURI also supports MockMvc's default empty servletPath and context-path deployments.
        admin = admin || request.getRequestURI().startsWith(request.getContextPath() + "/admin/");
        if (page < 0 || keyword.length() > 120 || !Set.of("", "Active", "Expired", "Pending").contains(card)
                || !Set.of("", "borrowed", "overdue", "none").contains(loan)
                || !Set.of("", "unpaid", "clear").contains(fine)
                || !Set.of("", "ACTIVE", "SUSPENDED", "EXPIRED").contains(status)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
        model.addAttribute("directory", directory.directory(admin, demo, keyword.trim(), card, loan, fine, status, page));
        common(model, admin, demo);
        model.addAttribute("keyword", keyword.trim());
        model.addAttribute("card", card);
        model.addAttribute("loan", loan);
        model.addAttribute("fine", fine);
        model.addAttribute("accountStatus", status);
        return "member/directory";
    }

    @GetMapping({"/staff/members/{id}", "/admin/users/{id}"})
    @PreAuthorize("hasAuthority('MEMBER_READ')")
    public String detail(@PathVariable Long id, @RequestParam(defaultValue = "false") boolean demo,
                         jakarta.servlet.http.HttpServletRequest request, Model model) {
        boolean admin = request.getRequestURI().startsWith(request.getContextPath() + "/admin/");
        var person = directory.detail(admin, demo, id);
        model.addAttribute("member", person);
        if (!model.containsAttribute("accessForm")) model.addAttribute("accessForm", new grp3.java.booknet.dto.member.MemberForms.AccessForm(
                person.access().equals("ACTIVE"), person.revision()));
        if (!model.containsAttribute("statusForm")) model.addAttribute("statusForm", new grp3.java.booknet.dto.member.MemberForms.StatusForm(
                person.access().equals("EXPIRED") ? grp3.java.booknet.enums.user.UserStatus.EXPIRED
                        : person.access().equals("SUSPENDED")
                                ? grp3.java.booknet.enums.user.UserStatus.SUSPENDED : grp3.java.booknet.enums.user.UserStatus.ACTIVE,
                person.revision()));
        if (!model.containsAttribute("activationForm")) model.addAttribute("activationForm", new grp3.java.booknet.dto.member.MemberForms.ActivationForm(false, null));
        model.addAttribute("loans", directory.previewLoans(demo, id));
        common(model, admin, demo);
        if(admin){if(!model.containsAttribute("profileForm"))model.addAttribute("profileForm",new grp3.java.booknet.dto.user.request.ProfileUpdateRequest(person.fullName(),person.phone(),person.address(),demo ? null : accounts.profile(id).avatarUrl()));model.addAttribute("activities",audits.findAll(org.springframework.data.domain.Sort.by("createdAt").descending()).stream().filter(a->a.getTargetId().equals(id)).limit(20).toList());return "member/admin-details";}
        return "member/details";
    }

    @PostMapping("/admin/users/{id}/access")
    @PreAuthorize("hasAuthority('USER_ACCESS_UPDATE')")
    public String access(@PathVariable Long id,
            @jakarta.validation.Valid @ModelAttribute("accessForm") grp3.java.booknet.dto.member.MemberForms.AccessForm form,
            org.springframework.validation.BindingResult errors, org.springframework.security.core.Authentication auth,
            jakarta.servlet.http.HttpServletRequest request, Model model,
            org.springframework.web.servlet.mvc.support.RedirectAttributes redirect) {
        if (!errors.hasErrors()) {
            try {
                administration.changeAccess(grp3.java.booknet.security.principal.AccountPrincipal.id(auth), id, form.enabled(), form.revision());
                redirect.addFlashAttribute("successMessage", "Account access updated. Existing sessions have been revoked.");
                return "redirect:/admin/users/" + id;
            } catch (IllegalArgumentException ex) { errors.reject("access.invalid", ex.getMessage()); }
        }
        model.addAttribute("accessForm", form);
        return detail(id, false, request, model);
    }

    @PostMapping("/admin/users/{id}/status")
    @PreAuthorize("hasAuthority('USER_ACCESS_UPDATE')")
    public String status(@PathVariable Long id,
            @jakarta.validation.Valid @ModelAttribute("statusForm") grp3.java.booknet.dto.member.MemberForms.StatusForm form,
            org.springframework.validation.BindingResult errors, org.springframework.security.core.Authentication auth,
            jakarta.servlet.http.HttpServletRequest request, Model model,
            org.springframework.web.servlet.mvc.support.RedirectAttributes redirect) {
        if (!errors.hasErrors()) {
            try {
                administration.changeStatus(grp3.java.booknet.security.principal.AccountPrincipal.id(auth), id, form.status(), form.revision());
                redirect.addFlashAttribute("successMessage", "Account status updated. Existing sessions have been revoked.");
                return "redirect:/admin/users/" + id;
            } catch (IllegalArgumentException ex) { errors.reject("status.invalid", ex.getMessage()); }
        }
        model.addAttribute("statusForm", form);
        return detail(id, false, request, model);
    }

    @PostMapping({"/staff/members/{id}/activate", "/admin/users/{id}/activate"})
    @PreAuthorize("hasAuthority('MEMBER_CARD_ACTIVATE')")
    public String activate(@PathVariable Long id,
            @jakarta.validation.Valid @ModelAttribute("activationForm") grp3.java.booknet.dto.member.MemberForms.ActivationForm form,
            org.springframework.validation.BindingResult errors, org.springframework.security.core.Authentication auth,
            jakarta.servlet.http.HttpServletRequest request, Model model,
            org.springframework.web.servlet.mvc.support.RedirectAttributes redirect) {
        if (!errors.hasErrors()) {
            try {
                cards.activate(grp3.java.booknet.security.principal.AccountPrincipal.id(auth), id, form);
                redirect.addFlashAttribute("successMessage", "Member card activated after identity verification.");
                return "redirect:" + (request.getRequestURI().startsWith(request.getContextPath() + "/admin/")
                        ? "/admin/users/" : "/staff/members/") + id;
            } catch (IllegalArgumentException ex) { errors.reject("card.invalid", ex.getMessage()); }
        }
        model.addAttribute("activationForm", form);
        return detail(id, false, request, model);
    }

    @PostMapping("/admin/users/{id}/control/{action}")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('USER_ACCESS_UPDATE')")
    public String control(@PathVariable Long id,@PathVariable String action,
        @jakarta.validation.Valid @ModelAttribute("controlForm") grp3.java.booknet.dto.member.AdminControlForm form,
        org.springframework.validation.BindingResult errors,org.springframework.security.core.Authentication auth,
        jakarta.servlet.http.HttpServletRequest request,Model model,org.springframework.web.servlet.mvc.support.RedirectAttributes redirect){
        if(!errors.hasErrors())try{administration.control(grp3.java.booknet.security.principal.AccountPrincipal.id(auth),id,action,form);
        redirect.addFlashAttribute("successMessage","Account action completed.");return "redirect:/admin/users/"+id;
        }catch(IllegalArgumentException|grp3.java.booknet.exception.account.AccountException ex){errors.reject("control.invalid",ex.getMessage());}
        model.addAttribute("controlErrors",errors.getAllErrors());return detail(id,false,request,model);
    }
    @PostMapping("/admin/users/{id}/profile")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('USER_ACCESS_UPDATE')")
    public String profile(@PathVariable Long id,@jakarta.validation.Valid @ModelAttribute("profileForm") grp3.java.booknet.dto.user.request.ProfileUpdateRequest form,
        org.springframework.validation.BindingResult errors,org.springframework.security.core.Authentication auth,jakarta.servlet.http.HttpServletRequest request,
        Model model,org.springframework.web.servlet.mvc.support.RedirectAttributes redirect){
        if(!errors.hasErrors())try{administration.updateProfile(grp3.java.booknet.security.principal.AccountPrincipal.id(auth),id,form);
            redirect.addFlashAttribute("successMessage","Profile updated.");return "redirect:/admin/users/"+id;
        }catch(IllegalArgumentException|grp3.java.booknet.exception.account.AccountException ex){errors.reject("profile.invalid",ex.getMessage());}
        model.addAttribute("profileEditing",true);return detail(id,false,request,model);
    }
    private void common(Model model, boolean admin, boolean demo) {
        model.addAttribute("adminArea", admin);
        model.addAttribute("demo", demo);
        model.addAttribute("basePath", admin ? "/admin/users" : "/staff/members");
        model.addAttribute("activePage", admin ? "users" : "members");
    }
}
