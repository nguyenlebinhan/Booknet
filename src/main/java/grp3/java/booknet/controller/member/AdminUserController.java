package grp3.java.booknet.controller.member;
import grp3.java.booknet.dto.member.*;
import grp3.java.booknet.entity.user.*;
import grp3.java.booknet.enums.user.*;
import grp3.java.booknet.service.member.*;
import grp3.java.booknet.repository.user.*;
import grp3.java.booknet.security.principal.AccountPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.*;
@Controller @RequiredArgsConstructor @PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {
 private final AccountAdministrationService administration;
 private final RoleManagementService roleService;
 @InitBinder("createForm") void bind(org.springframework.web.bind.WebDataBinder binder){binder.setDeclarativeBinding(true);}
 private void choices(Model model){model.addAttribute("roles",roleService.all().stream().filter(r->r.getSystemRole()!=UserRole.ROLE_GUEST).toList());}
 @GetMapping("/admin/users/create") public String create(Model model){
 choices(model);model.addAttribute("createForm",new InviteAccountForm("","","",null,""));return "member/create-librarian";}
 @PostMapping("/admin/users/create") public String save(@Valid @ModelAttribute("createForm") InviteAccountForm form,BindingResult errors,Authentication auth,Model model,RedirectAttributes redirect){
 if(!errors.hasErrors())try{
 Long id=administration.inviteAccount(AccountPrincipal.id(auth),form);
 redirect.addFlashAttribute("successMessage","Account created and invitation sent. The recipient has 24 hours to activate it.");return "redirect:/admin/users/"+id;
 }catch(IllegalArgumentException | grp3.java.booknet.exception.account.AccountException ex){errors.reject("create.failed",ex.getMessage());}
 catch(org.springframework.dao.DataIntegrityViolationException ex){errors.reject("duplicate","This email address is already registered.");}
 choices(model);return "member/create-librarian";}
 @GetMapping("/admin/roles") public String roles(Model model){
 model.addAttribute("roles",roleService.all());
 model.addAttribute("comparisons",PermissionComparison.rows());
 return "member/roles";}
 // Older bookmarks return to the single comparison page.
 @GetMapping({"/admin/roles/select","/admin/roles/create","/admin/roles/{id}/edit"})
 public String formerRoleViews(){return "redirect:/admin/roles";}
 // Keep former write URLs explicitly closed for stale pages and crafted requests.
 @PostMapping({"/admin/roles/create","/admin/roles/{id}/edit"})
 @PreAuthorize("denyAll()")
 public void readOnlyRoles(){throw new org.springframework.security.access.AccessDeniedException("Role permissions are read-only.");}
}
