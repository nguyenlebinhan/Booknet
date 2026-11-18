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
 private final AccountAccessAuditRepository audits;
 @InitBinder({"createForm","roleForm"}) void bind(org.springframework.web.bind.WebDataBinder binder){binder.setDeclarativeBinding(true);}
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
 private void roleData(Model model){
 var roles=roleService.all();model.addAttribute("roles",roles.stream().filter(r->r.getSystemRole()!=UserRole.ROLE_GUEST).toList());
 Map<Long,Long> counts=new HashMap<>();roles.forEach(r->counts.put(r.getId(),roleService.count(r)));model.addAttribute("counts",counts);
 model.addAttribute("capabilities",RoleCapability.values());
 model.addAttribute("comparisons",PermissionComparison.rows());
 }
 @GetMapping("/admin/roles") public String roles(@RequestParam(required=false) Long selected,Model model){
 roleData(model);var roles=roleService.all();model.addAttribute("selected",selected==null?roles.stream().filter(r->r.getSystemRole()==UserRole.ROLE_LIBRARIAN).findFirst().orElseThrow():roleService.get(selected));
 model.addAttribute("activities",audits.findAll(org.springframework.data.domain.Sort.by("createdAt").descending()).stream().filter(a->a.getAction().startsWith("ROLE_")).limit(10).toList());
 return "member/roles";}
 @GetMapping("/admin/roles/select") public String select(Model model){roleData(model);return "member/role-select";}
 @GetMapping("/admin/roles/create") public String newRole(Model model){roleData(model);model.addAttribute("editing",false);model.addAttribute("roleForm",new RoleForm("","",Set.of(),0));return "member/role-create";}
 @PostMapping("/admin/roles/create") public String createRole(@Valid @ModelAttribute("roleForm") RoleForm form,BindingResult errors,Authentication auth,Model model,RedirectAttributes redirect){return persistRole(null,form,errors,auth,model,redirect);}
 @GetMapping("/admin/roles/{id}/edit") public String editRole(@PathVariable Long id,Model model){
 var role=roleService.get(id);roleData(model);model.addAttribute("role",role);model.addAttribute("editing",true);
 model.addAttribute("roleForm",new RoleForm(role.getName(),role.getDescription(),role.getCapabilities(),role.getRevision()));return "member/role-edit";}
 @PostMapping("/admin/roles/{id}/edit") public String saveRole(@PathVariable Long id,@Valid @ModelAttribute("roleForm") RoleForm form,BindingResult errors,Authentication auth,Model model,RedirectAttributes redirect){return persistRole(id,form,errors,auth,model,redirect);}
 private String persistRole(Long id,RoleForm form,BindingResult errors,Authentication auth,Model model,RedirectAttributes redirect){
 if(!errors.hasErrors())try{Long saved=roleService.save(id,form,AccountPrincipal.id(auth));redirect.addFlashAttribute("successMessage","Role permissions saved. Affected sessions must sign in again.");return "redirect:/admin/roles?selected="+saved;}
 catch(IllegalArgumentException|org.springframework.orm.ObjectOptimisticLockingFailureException ex){errors.reject("role.invalid",ex instanceof IllegalArgumentException?ex.getMessage():"This role changed. Reload before saving.");}
 roleData(model);model.addAttribute("editing",id!=null);if(id!=null)model.addAttribute("role",roleService.get(id));return id==null?"member/role-create":"member/role-edit";
 }
}
