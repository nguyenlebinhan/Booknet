package grp3.java.booknet;
import grp3.java.booknet.entity.user.*;
import grp3.java.booknet.enums.user.*;
import grp3.java.booknet.repository.user.*;
import grp3.java.booknet.security.dto.CustomUserDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.test.web.servlet.MockMvc;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import java.util.*;
import java.time.Duration;
import java.nio.file.*;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test")
class AdminRoleInvitationTests {
 @Autowired MockMvc mvc; @Autowired UserRepository users; @Autowired RoleDefinitionRepository roles;
 @Autowired grp3.java.booknet.service.member.RoleManagementService roleService;
 @Autowired AccountTokenRepository tokens; @Autowired OAuthIdentityRepository identities; @Autowired AccountAccessAuditRepository audits;
 @MockitoBean JavaMailSender mail;
 User admin,member;
 @BeforeEach void setup(){tokens.deleteAll();identities.deleteAll();audits.deleteAll();users.deleteAll();reset(mail);
 for(var role:roles.findAll())if(role.getSystemRole()!=null){role.setCapabilities(new HashSet<>(UserRole.defaults(role.getSystemRole())));roles.saveAndFlush(role);}
 admin=save("admin@example.com",UserRole.ROLE_ADMIN);member=save("member@example.com",UserRole.ROLE_USER);}
 User save(String email,UserRole role){return users.saveAndFlush(User.builder().email(email).fullName("Test Account").provider(AuthProvider.LOCAL).role(role).enabled(true).emailVerified(true).status(UserStatus.ACTIVE).build());}
 Long saveRole(Long id,grp3.java.booknet.dto.member.RoleForm form,Long actor){
 var context=org.springframework.security.core.context.SecurityContextHolder.createEmptyContext();
 var principal=new CustomUserDetails(admin);context.setAuthentication(org.springframework.security.authentication.UsernamePasswordAuthenticationToken.authenticated(principal,null,principal.getAuthorities()));
 org.springframework.security.core.context.SecurityContextHolder.setContext(context);
 try{return roleService.save(id,form,actor);}finally{org.springframework.security.core.context.SecurityContextHolder.clearContext();}
 }
 @AfterEach void resetPolicies(){for(var role:roles.findAll())if(role.getSystemRole()!=null){role.setCapabilities(new HashSet<>(UserRole.defaults(role.getSystemRole())));roles.saveAndFlush(role);}}
 @Test void pagesRenderAndProduceReviewArtifacts()throws Exception{
 var pages=List.of("/admin/users/create","/admin/roles","/admin/users/"+member.getId());
 for(String path:pages){
 String html=mvc.perform(get(path).with(user(new CustomUserDetails(admin)))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
 assertThat(html).doesNotContain("th:field","th:each");
 assertThat(html).contains("class=\"admin-sidebar\"", "href=\"/admin/users\"", "href=\"/admin/roles\"");
 if(path.startsWith("/admin/roles")) assertThat(html).contains("aria-current=\"page\"");
 if(path.equals("/admin/roles")) {
 assertThat(html).contains("Guest","FUNCTION &amp; ACCESS SCOPE","Submit a borrowing request","Allowed","Not allowed");
 assertThat(html).doesNotContain("View roles</a>","View audit log</a>","Recent role activity","id=\"activity\"","/admin/roles/select");
 assertThat(html).doesNotContainPattern("<td[^>]*>\\s*(Own|All members|Partial)\\s*</td>");
 }

 Path out=Path.of("target/admin-ui-review");Files.createDirectories(out);
 String assets=Path.of("src/main/resources/static").toAbsolutePath().toUri().toString();
 Files.writeString(out.resolve(path.replace('/','_')+".html"),html.replace("href=\"/css/","href=\""+assets+"css/").replace("src=\"/js/","src=\""+assets+"js/").replace("src=\"/images/","src=\""+assets+"images/"));
 }
 }
 @Test void formerRolePagesRedirectToTheSingleComparison()throws Exception{
 var role=roles.findBySystemRole(UserRole.ROLE_LIBRARIAN).orElseThrow();
 for(String path:List.of("/admin/roles/select","/admin/roles/create","/admin/roles/"+role.getId()+"/edit")){
 mvc.perform(get(path).with(user(new CustomUserDetails(admin)))).andExpect(redirectedUrl("/admin/roles"));
 mvc.perform(get(path).with(user(new CustomUserDetails(member)))).andExpect(status().isForbidden());
 }
 var response=mvc.perform(get("/admin/roles").with(user(new CustomUserDetails(admin)))).andExpect(status().isOk()).andReturn();
 assertThat(response.getModelAndView().getModel()).doesNotContainKeys("activities","counts","selected");
 }
 @Test void invitationSetsPasswordOnceAndKeepsAssignedRole()throws Exception{
 var lib=roles.findBySystemRole(UserRole.ROLE_LIBRARIAN).orElseThrow();
 mvc.perform(post("/admin/users/create").with(user(new CustomUserDetails(admin))).with(csrf())
 .param("fullName","Invited Librarian").param("email","invite@example.com").param("phone","")
 .param("roleId",lib.getId().toString()).param("personalMessage","Welcome to the library").param("role","ROLE_ADMIN"))
 .andExpect(status().is3xxRedirection());
 var invited=users.findByEmail("invite@example.com").orElseThrow();assertThat(invited.getRole()).isEqualTo(UserRole.ROLE_LIBRARIAN);assertThat(invited.hasPassword()).isFalse();assertThat(invited.isEmailVerified()).isFalse();
 var token=tokens.findAll().getFirst();assertThat(token.getPurpose()).isEqualTo(AccountTokenPurpose.INVITE_ACCOUNT);assertThat(Duration.between(token.getIssuedAt(),token.getExpiresAt())).isEqualTo(Duration.ofHours(24));
 var capture=ArgumentCaptor.forClass(SimpleMailMessage.class);verify(mail).send(capture.capture());String text=capture.getValue().getText();assertThat(text).contains("Welcome to the library","valid for 24 hours");
 String raw=text.substring(text.indexOf("token=")+6).split("\\s")[0];
 mvc.perform(get("/reset-password").param("token",raw)).andExpect(model().attribute("tokenValid",true));
 mvc.perform(post("/reset-password").with(csrf()).param("token",raw).param("password","NewPass123").param("confirmPassword","NewPass123")).andExpect(redirectedUrl("/login?reset=true"));
 invited=users.findById(invited.getId()).orElseThrow();assertThat(invited.isEmailVerified()).isTrue();assertThat(invited.hasPassword()).isTrue();assertThat(invited.getRole()).isEqualTo(UserRole.ROLE_LIBRARIAN);
 mvc.perform(get("/reset-password").param("token",raw)).andExpect(model().attribute("tokenValid",false));
 mvc.perform(post("/login").with(csrf()).param("email","invite@example.com").param("password","NewPass123")).andExpect(redirectedUrl("/staff/members"));
 }
 @Test
 void rolePagesAreReadOnlyAndExistingCustomRolesStillEnforceAccess()throws Exception{
 mvc.perform(post("/admin/roles/create").with(user(new CustomUserDetails(member))).with(csrf()).param("name","Denied")).andExpect(status().isForbidden());
 mvc.perform(get("/admin/roles/create").with(user(new CustomUserDetails(admin)))).andExpect(redirectedUrl("/admin/roles"));
 mvc.perform(post("/admin/roles/create").with(user(new CustomUserDetails(admin))).with(csrf()).param("name","Denied")).andExpect(status().isForbidden());
 assertThatThrownBy(()->saveRole(null,new grp3.java.booknet.dto.member.RoleForm("Escalation","",Set.of(RoleCapability.ROLE_MANAGE),0),admin.getId())).isInstanceOf(IllegalArgumentException.class);
 Long id=saveRole(null,new grp3.java.booknet.dto.member.RoleForm("Catalog Assistant","Catalog only",Set.of(RoleCapability.CATALOG_EDIT,RoleCapability.INVENTORY_REPORT),0),admin.getId());
 var role=roles.findById(id).orElseThrow();assertThat(role.getCapabilities()).containsExactlyInAnyOrder(RoleCapability.CATALOG_EDIT,RoleCapability.INVENTORY_REPORT);
 member.setAssignedRole(role);users.saveAndFlush(member);
 mvc.perform(get("/admin/books/create").with(user(new CustomUserDetails(member)))).andExpect(status().isOk());
 mvc.perform(get("/admin/users").with(user(new CustomUserDetails(member)))).andExpect(status().isForbidden());
 mvc.perform(get("/staff/members").with(user(new CustomUserDetails(member)))).andExpect(status().isForbidden());
 }
 @Test
 void roleWritesAreBlockedAndInternalPermissionChangesStillRevokeSessions()throws Exception{
 var lib=save("lib@example.com",UserRole.ROLE_LIBRARIAN);var stale=new CustomUserDetails(lib);var role=roles.findBySystemRole(UserRole.ROLE_LIBRARIAN).orElseThrow();
 mvc.perform(post("/admin/roles/"+role.getId()+"/edit").with(user(new CustomUserDetails(admin))).with(csrf()).param("name",role.getName()).param("description",role.getDescription()).param("revision",Long.toString(role.getRevision())).param("capabilities","CATALOG_EDIT")).andExpect(status().isForbidden());
 assertThat(roles.findById(role.getId()).orElseThrow().getCapabilities()).contains(RoleCapability.MEMBER_READ);
 saveRole(role.getId(),new grp3.java.booknet.dto.member.RoleForm(role.getName(),role.getDescription(),Set.of(RoleCapability.CATALOG_EDIT),role.getRevision()),admin.getId());
 mvc.perform(get("/staff/members").with(user(stale))).andExpect(redirectedUrl("/login?error=true"));
 lib=users.findById(lib.getId()).orElseThrow();mvc.perform(get("/staff/members").with(user(new CustomUserDetails(lib)))).andExpect(status().isForbidden());
 var ar=roles.findBySystemRole(UserRole.ROLE_ADMIN).orElseThrow();
 mvc.perform(post("/admin/roles/"+ar.getId()+"/edit").with(user(new CustomUserDetails(admin))).with(csrf()).param("name",ar.getName()).param("revision",Long.toString(ar.getRevision()))).andExpect(status().isForbidden());
 saveRole(ar.getId(),new grp3.java.booknet.dto.member.RoleForm(ar.getName(),ar.getDescription(),Set.of(),ar.getRevision()),admin.getId());
 assertThat(roles.findById(ar.getId()).orElseThrow().getCapabilities()).contains(RoleCapability.ROLE_MANAGE,RoleCapability.USER_ACCESS_UPDATE,RoleCapability.MEMBER_READ,RoleCapability.MEMBER_CARD_ACTIVATE);
 }
 @Test void controlRequiresCsrfReasonConfirmationAndAudits()throws Exception{
 mvc.perform(post("/admin/users/"+member.getId()+"/control/lock").with(user(new CustomUserDetails(admin)))).andExpect(status().isForbidden());
 mvc.perform(post("/admin/users/"+member.getId()+"/control/lock").with(user(new CustomUserDetails(admin))).with(csrf()).param("reason","Review").param("confirmed","false").param("revision","0")).andExpect(status().isOk());
 assertThat(users.findById(member.getId()).orElseThrow().isEnabled()).isTrue();
 mvc.perform(post("/admin/users/"+member.getId()+"/control/lock").with(user(new CustomUserDetails(admin))).with(csrf()).param("reason","Identity review").param("confirmed","true").param("revision","0")).andExpect(status().is3xxRedirection());
 assertThat(users.findById(member.getId()).orElseThrow().isEnabled()).isFalse();assertThat(audits.findAll()).singleElement().satisfies(a->assertThat(a.getReason()).isEqualTo("Identity review"));
 }
 @Test void profileErrorsRemainVisibleAndSavingCannotChangeRole()throws Exception{
 String path="/admin/users/"+member.getId()+"/profile";
 mvc.perform(post(path).with(user(new CustomUserDetails(admin))).param("fullName","Updated Member")).andExpect(status().isForbidden());
 mvc.perform(post(path).with(user(new CustomUserDetails(admin))).with(csrf()).param("fullName","").param("phone","abc"))
 .andExpect(status().isOk()).andExpect(model().attributeHasErrors("profileForm"))
 .andExpect(model().attribute("profileEditing",true)).andExpect(content().string(org.hamcrest.Matchers.containsString("class=\"ad-profile-editor\"")));
 mvc.perform(post(path).with(user(new CustomUserDetails(admin))).with(csrf()).param("fullName","Updated Member").param("phone","0901234567").param("address","Library Street").param("avatarUrl","").param("role","ROLE_ADMIN"))
 .andExpect(redirectedUrl("/admin/users/"+member.getId()));
 var updated=users.findById(member.getId()).orElseThrow();assertThat(updated.getFullName()).isEqualTo("Updated Member");assertThat(updated.getRole()).isEqualTo(UserRole.ROLE_USER);
 }
 @Test void duplicateAndSmtpFailureDoNotCreateAccounts()throws Exception{
 var role=roles.findBySystemRole(UserRole.ROLE_USER).orElseThrow();
 mvc.perform(post("/admin/users/create").with(user(new CustomUserDetails(admin))).with(csrf()).param("fullName","Duplicate Account").param("email",member.getEmail()).param("roleId",role.getId().toString())).andExpect(status().isOk()).andExpect(model().attributeHasErrors("createForm"));
 verifyNoInteractions(mail);
 doThrow(new org.springframework.mail.MailSendException("offline")).when(mail).send(any(SimpleMailMessage.class));
 mvc.perform(post("/admin/users/create").with(user(new CustomUserDetails(admin))).with(csrf()).param("fullName","Pending Account").param("email","offline@example.com").param("roleId",role.getId().toString())).andExpect(status().isOk()).andExpect(model().attributeHasErrors("createForm"));
 assertThat(users.existsByEmail("offline@example.com")).isFalse();assertThat(tokens.count()).isZero();
 }
 @Test void expiredInvitationCannotActivateAndAccountRoleCannotBeChanged()throws Exception{
 var role=roles.findBySystemRole(UserRole.ROLE_LIBRARIAN).orElseThrow();
 mvc.perform(post("/admin/users/create").with(user(new CustomUserDetails(admin))).with(csrf()).param("fullName","Expired Invite").param("email","expired@example.com").param("roleId",role.getId().toString())).andExpect(status().is3xxRedirection());
 var capture=ArgumentCaptor.forClass(SimpleMailMessage.class);verify(mail).send(capture.capture());
 String text=capture.getValue().getText();String raw=text.substring(text.indexOf("token=")+6).split("\\s")[0];
 var token=tokens.findAll().getFirst();token.setExpiresAt(java.time.Instant.now().minusSeconds(1));tokens.saveAndFlush(token);
 mvc.perform(get("/reset-password").param("token",raw)).andExpect(model().attribute("tokenValid",false));
 mvc.perform(post("/reset-password").with(csrf()).param("token",raw).param("password","NewPass123").param("confirmPassword","NewPass123")).andExpect(status().isOk());
 assertThat(users.findByEmail("expired@example.com").orElseThrow().isEmailVerified()).isFalse();
 mvc.perform(post("/admin/users/"+member.getId()+"/role").with(user(new CustomUserDetails(admin))).with(csrf()).param("role","ROLE_ADMIN")).andExpect(status().isNotFound());
 mvc.perform(post("/admin/users/"+member.getId()+"/status").with(user(new CustomUserDetails(admin))).with(csrf()).param("status","SUSPENDED").param("revision","0").param("role","ROLE_ADMIN")).andExpect(status().is3xxRedirection());
 assertThat(users.findById(member.getId()).orElseThrow().getRole()).isEqualTo(UserRole.ROLE_USER);
 }
}
