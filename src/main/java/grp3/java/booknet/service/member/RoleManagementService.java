package grp3.java.booknet.service.member;
import grp3.java.booknet.entity.user.*;
import grp3.java.booknet.enums.user.*;
import grp3.java.booknet.dto.member.RoleForm;
import grp3.java.booknet.repository.user.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;
@Service @RequiredArgsConstructor
public class RoleManagementService {
 private final RoleDefinitionRepository roles; private final UserRepository users; private final AccountAccessAuditRepository audits;
 @Transactional public void initialize(){
 for(var role:UserRole.values())if(roles.findBySystemRole(role).isEmpty()){
 var def=new RoleDefinition();def.setName(switch(role){case ROLE_USER->"Member";case ROLE_ADMIN->"Administrator";case ROLE_LIBRARIAN->"Librarian";case ROLE_GUEST->"Guest";});
 def.setSystemRole(role);def.setDescription(switch(role){case ROLE_USER->"Manage a personal profile and borrowing requests. No access to other members or staff tools.";case ROLE_LIBRARIAN->"Work with the catalog, circulation and all-member service records. No role or policy administration.";case ROLE_ADMIN->"Manage library operations with protected access to user accounts, roles, policies and the audit log.";case ROLE_GUEST->"Browse public pages without an account.";});
 def.setCapabilities(new HashSet<>(UserRole.defaults(role)));roles.save(def);
 }}
 @Transactional(readOnly=true) @PreAuthorize("hasRole('ADMIN')")
 public List<RoleDefinition> all(){return roles.findAll().stream().sorted(Comparator.comparing(RoleDefinition::getId)).toList();}
 @Transactional(readOnly=true) @PreAuthorize("hasRole('ADMIN')")
 public RoleDefinition get(Long id){return roles.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));}
 @Transactional @PreAuthorize("hasRole('ADMIN') and hasAuthority('ROLE_MANAGE')")
 public Long save(Long id,RoleForm form,Long actor){
 RoleDefinition role=id==null?new RoleDefinition():get(id);
 if(id!=null && role.getRevision()!=form.revision())throw new IllegalArgumentException("This role changed. Reload before saving.");
 String name=form.name().trim();
 if(name.length()<2)throw new IllegalArgumentException("Role name must contain at least two characters.");
 if(role.getSystemRole()!=null && !role.getName().equals(name))throw new IllegalArgumentException("System role names cannot be changed.");
 if(!name.equalsIgnoreCase(role.getName()) && roles.existsByNameIgnoreCase(name))throw new IllegalArgumentException("This role name already exists.");
 Set<RoleCapability> selected=EnumSet.noneOf(RoleCapability.class);if(form.capabilities()!=null)selected.addAll(form.capabilities());
 var kind=role.getSystemRole();
 if(kind==UserRole.ROLE_GUEST && !selected.isEmpty())throw new IllegalArgumentException("Guest access is public and cannot be elevated.");
 if(kind==UserRole.ROLE_USER && selected.stream().anyMatch(p->!Set.of(RoleCapability.PROFILE_UPDATE,RoleCapability.LOAN_REQUEST,RoleCapability.RESERVE,RoleCapability.RENEW).contains(p)))throw new IllegalArgumentException("Members cannot receive staff permissions.");
 if(kind!=UserRole.ROLE_ADMIN && selected.stream().anyMatch(RoleCapability::administrative))throw new IllegalArgumentException("Administrative permissions are reserved for the Administrator role.");
 if(id!=null && (kind==UserRole.ROLE_ADMIN || kind==UserRole.ROLE_LIBRARIAN)){selected.add(RoleCapability.PROFILE_UPDATE);
 if(selected.contains(RoleCapability.CATALOG_EDIT))selected.addAll(Set.of(RoleCapability.COPY_MANAGE,RoleCapability.BOOK_RETIRE));
 if(selected.contains(RoleCapability.MEMBER_READ))selected.add(RoleCapability.MEMBER_CARD_ACTIVATE);
 if(selected.contains(RoleCapability.INVENTORY_REPORT))selected.add(RoleCapability.CIRCULATION_REPORT);}
 if(kind==UserRole.ROLE_ADMIN){for(var p:RoleCapability.values())if(p.administrative())selected.add(p);
 selected.addAll(Set.of(RoleCapability.MEMBER_READ,RoleCapability.MEMBER_CARD_ACTIVATE,RoleCapability.FINE_PAYMENT));}
 role.setName(name);role.setDescription(form.description());role.setCapabilities(new HashSet<>(selected));roles.saveAndFlush(role);
 for(var user:users.findAll())if((user.getAssignedRole()!=null && role.getId().equals(user.getAssignedRole().getId())) || (user.getAssignedRole()==null && kind!=null && user.getRole()==kind)){
 user.setAssignedRole(role);user.setAuthVersion(user.getAuthVersion()+1);}
 var audit=new AccountAccessAudit();audit.setActorId(actor);audit.setTargetId(actor);audit.setAction(id==null?"ROLE_CREATE":"ROLE_PERMISSIONS");audit.setPreviousValue("role:"+role.getId());audit.setNewValue(name);audits.save(audit);
 return role.getId();
 }
 @Transactional(readOnly=true) public long count(RoleDefinition role){return users.findAll().stream().filter(u->u.getAssignedRole()!=null?u.getAssignedRole().getId().equals(role.getId()):role.getSystemRole()!=null && u.getRole()==role.getSystemRole()).count();}
}
