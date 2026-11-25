package grp3.java.booknet;

import grp3.java.booknet.entity.user.User;
import grp3.java.booknet.enums.user.*;
import grp3.java.booknet.repository.user.*;
import grp3.java.booknet.security.dto.CustomUserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MemberManagementUiTests {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired AccountTokenRepository tokens;
    @Autowired OAuthIdentityRepository identities;
    @Autowired MemberCardRepository cards;
    @Autowired AccountAccessAuditRepository audits;
    @MockitoBean JavaMailSender sender;
    User librarian;
    User admin;
    User member;

    @BeforeEach
    void prepare() {
        tokens.deleteAll();
        identities.deleteAll();
        audits.deleteAll();
        users.deleteAll();
        librarian = save("Library Staff", "staff@example.com", UserRole.ROLE_LIBRARIAN);
        admin = save("Library Admin", "admin@example.com", UserRole.ROLE_ADMIN);
        member = save("Actual Reader", "reader@example.com", UserRole.ROLE_USER);
        member.setPhone("555-0100");
        member = users.saveAndFlush(member);
    }

    @Test
    void liveDirectoryUsesRealMembersAndNeverFillsMissingValuesWithDemoData() throws Exception {
        String html = mvc.perform(get("/staff/members").with(user(new CustomUserDetails(librarian))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(html).contains("Actual Reader", "LIVE DIRECTORY", "Pending", "Showing 1–1 of 1 records")
                .contains("class=\"admin-sidebar\"", "STAFF SPACE", "href=\"/staff/members\"")
                .doesNotContain("Nguyễn Minh Anh", "TV-02481", "Library Admin", "th:each");
        mvc.perform(get("/staff/members").param("keyword", "nothing").with(user(new CustomUserDetails(librarian))))
                .andExpect(content().string(containsString("No matching records")));
        mvc.perform(get("/staff/members").param("keyword", "reader@example.com").with(user(new CustomUserDetails(librarian))))
                .andExpect(content().string(containsString("Actual Reader")));
        mvc.perform(get("/staff/members").param("keyword", "555-0100").with(user(new CustomUserDetails(librarian))))
                .andExpect(status().isOk()).andExpect(content().string(containsString("Actual Reader")));
    }

    @Test
    void staffGetsSidebarWhileMembersAndGuestsKeepNavbar() throws Exception {
        String librarianCatalog = mvc.perform(get("/catalog").with(user(new CustomUserDetails(librarian))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(librarianCatalog).contains("class=\"admin-sidebar\"", "STAFF SPACE")
                .doesNotContain("id=\"booknet-navbar\"", "href=\"/admin/dashboard\"", "href=\"/admin/users\"");

        String adminCatalog = mvc.perform(get("/catalog").with(user(new CustomUserDetails(admin))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(adminCatalog).contains("class=\"admin-sidebar\"", "ADMIN SPACE")
                .doesNotContain("id=\"booknet-navbar\"");

        String memberCatalog = mvc.perform(get("/catalog").with(user(new CustomUserDetails(member))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(memberCatalog).contains("id=\"booknet-navbar\"")
                .doesNotContain("class=\"admin-sidebar\"");

        String guestCatalog = mvc.perform(get("/catalog"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(guestCatalog).contains("id=\"booknet-navbar\"")
                .doesNotContain("class=\"admin-sidebar\"");
    }

    @Test
    void demoFiltersRunBeforePaginationAndKeepTheirQueryParameters() throws Exception {
        var signedIn = user(new CustomUserDetails(librarian));
        mvc.perform(get("/staff/members").param("demo", "true").with(signedIn))
                .andExpect(status().isOk()).andExpect(content().string(containsString("Showing 1–7 of 8 records")));
        mvc.perform(get("/staff/members").param("demo", "true").param("page", "1").with(signedIn))
                .andExpect(content().string(containsString("Showing 8–8 of 8 records")))
                .andExpect(content().string(containsString("Đỗ An Nhiên")));
        String filtered = mvc.perform(get("/staff/members").param("demo", "true").param("card", "Active")
                        .param("loan", "overdue").param("fine", "unpaid").param("page", "99").with(signedIn))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(filtered).contains("Showing 1–1 of 1 records", "Nguyễn Minh Anh", "selected=\"selected\"")
                .doesNotContain("Trần Hoàng Nam");
        assertThat(users.count()).isEqualTo(3);
    }

    @Test
    void liveFiltersUseRealRecordsAndSearchIsEscaped() throws Exception {
        var signedIn = user(new CustomUserDetails(librarian));
        mvc.perform(get("/staff/members").param("loan", "overdue").with(signedIn)).andExpect(status().isOk())
                .andExpect(content().string(containsString("No matching records")));
        mvc.perform(get("/staff/members").param("status", "SUSPENDED").with(signedIn)).andExpect(status().isBadRequest());
        mvc.perform(get("/staff/members").param("demo", "true").param("card", "unknown").with(signedIn))
                .andExpect(status().isBadRequest());
        String html = mvc.perform(get("/staff/members").param("demo", "true").param("keyword", "<script>alert(1)</script>").with(signedIn))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(html).doesNotContain("<script>alert(1)</script>").contains("&lt;script&gt;");
    }

    @Test
    void adminCanFilterAccountStatusWithoutMixingRoleAndCardStatus() throws Exception {
        member.setStatus(UserStatus.SUSPENDED);
        users.saveAndFlush(member);
        String html = mvc.perform(get("/admin/users").param("status", "SUSPENDED")
                        .with(user(new CustomUserDetails(admin))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(html).contains("Actual Reader", "name=\"status\"", "value=\"SUSPENDED\" selected=\"selected\"")
                .doesNotContain("Library Staff", "Library Admin");
    }

    @Test
    void membersCannotOpenStaffOrAdminPagesAndStaffCannotReadEmployeeDetails() throws Exception {
        for (String path : new String[]{"/staff/members", "/staff/members/2481?demo=true", "/admin/users"}) {
            mvc.perform(get(path).with(user(new CustomUserDetails(member)))).andExpect(status().isForbidden());
        }
        mvc.perform(get("/admin/users").with(user(new CustomUserDetails(librarian)))).andExpect(status().isForbidden());
        mvc.perform(get("/staff/members/" + admin.getId()).with(user(new CustomUserDetails(librarian))))
                .andExpect(status().isNotFound());
        mvc.perform(get("/staff/members/999999").param("demo", "true").with(user(new CustomUserDetails(librarian))))
                .andExpect(status().isNotFound());
    }

    @Test
    void detailShowsSampleLoansAndAdminControlsAreRestrictedAndCannotSubmitChanges() throws Exception {
        String staffHtml = mvc.perform(get("/staff/members/2481").param("demo", "true")
                        .with(user(new CustomUserDetails(librarian))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(staffHtml).contains("Dế Mèn phiêu lưu ký", "Return requested 07/11", "10.000đ", "15.000đ", "Activate member card")
                .doesNotContain("Save role", "Lock / unlock account");
        String adminHtml = mvc.perform(get("/admin/users/" + member.getId()).with(user(new CustomUserDetails(admin))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(adminHtml).contains("User account details", "Actual Reader", "The account role is fixed after creation.").doesNotContain("Save role");
        mvc.perform(post("/admin/users/" + member.getId() + "/role").with(user(new CustomUserDetails(admin))).with(csrf())
                .param("role","ROLE_ADMIN").param("revision","0")).andExpect(status().is4xxClientError());
        assertThat(users.findById(member.getId()).orElseThrow().getRole()).isEqualTo(UserRole.ROLE_USER);
    }

    @Test
    void adminCanLockAndUnlockAccountsAndCannotRemoveLastActiveAdministrator() throws Exception {
        var signedIn = user(new CustomUserDetails(admin));
        mvc.perform(post("/admin/users/" + member.getId() + "/access").with(signedIn).with(csrf())
                        .param("enabled", "false").param("revision", "0"))
                .andExpect(status().is3xxRedirection());
        User locked = users.findById(member.getId()).orElseThrow();
        assertThat(locked.isEnabled()).isFalse();
        assertThat(locked.getStatus()).isEqualTo(UserStatus.SUSPENDED);
        mvc.perform(post("/admin/users/" + member.getId() + "/access").with(signedIn).with(csrf())
                        .param("enabled", "true").param("revision", "1"))
                .andExpect(status().is3xxRedirection());
        assertThat(users.findById(member.getId()).orElseThrow().isEnabled()).isTrue();
        assertThat(audits.count()).isEqualTo(2);

        mvc.perform(post("/admin/users/" + librarian.getId() + "/role").with(signedIn).with(csrf())
                        .param("role", "ROLE_USER").param("revision", "0"))
                .andExpect(status().is4xxClientError());
        assertThat(users.findById(librarian.getId()).orElseThrow().getRole()).isEqualTo(UserRole.ROLE_LIBRARIAN);
    }

    @Test
    void librarianCanActivateVerifiedMemberCardAfterIdentityCheck() throws Exception {
        var signedIn = user(new CustomUserDetails(librarian));
        mvc.perform(post("/staff/members/" + member.getId() + "/activate").with(signedIn).with(csrf())
                        .param("identityChecked", "true").param("expiresOn", "2027-11-16"))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/staff/members/" + member.getId()));
        var card = cards.findByUserId(member.getId()).orElseThrow();
        assertThat(card.getIssuedOn()).isNotNull();
        assertThat(card.getExpiresOn().toString()).isEqualTo("2027-11-16");
        assertThat(card.getActivatedBy()).isEqualTo(librarian.getId());
    }

    @Test
    void adminCanSetExpiredStatusAndMemberEligibilityRequiresAnActiveCard() throws Exception {
        var signedIn = user(new CustomUserDetails(admin));
        mvc.perform(post("/admin/users/" + member.getId() + "/status").with(signedIn).with(csrf())
                        .param("status", "EXPIRED").param("revision", "0"))
                .andExpect(status().is3xxRedirection());
        User expired = users.findById(member.getId()).orElseThrow();
        assertThat(expired.getStatus()).isEqualTo(UserStatus.EXPIRED);
        assertThat(expired.isEnabled()).isTrue();
        assertThat(audits.findAll()).singleElement().satisfies(audit -> assertThat(audit.getAction()).isEqualTo("STATUS"));
    }

    @Test
    void activeStatusCannotBypassEmailVerificationOrAccountLock() throws Exception {
        member.setEmailVerified(false);
        member.setStatus(UserStatus.SUSPENDED);
        member.setEnabled(false);
        users.saveAndFlush(member);
        mvc.perform(post("/admin/users/" + member.getId() + "/status")
                        .with(user(new CustomUserDetails(admin))).with(csrf())
                        .param("status", "ACTIVE").param("revision", "0"))
                .andExpect(status().isOk()).andExpect(model().attributeHasErrors("statusForm"));
        User unchanged = users.findById(member.getId()).orElseThrow();
        assertThat(unchanged.isEnabled()).isFalse();
        assertThat(unchanged.getStatus()).isEqualTo(UserStatus.SUSPENDED);
    }


    @Test
    void rendersPagesForVisualReviewAndAccountKeepsItsWorkingForm() throws Exception {
        write("members", mvc.perform(get("/staff/members").param("demo", "true").with(user(new CustomUserDetails(librarian))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        write("details", mvc.perform(get("/staff/members/2481").param("demo", "true").with(user(new CustomUserDetails(admin))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        write("authorization", mvc.perform(get("/admin/users").with(user(new CustomUserDetails(admin))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        String account = mvc.perform(get("/account/profile").with(user(new CustomUserDetails(member))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(account).contains("name=\"fullName\"", "name=\"_csrf\"", "BN-", "Pending", "Card issued</small><span>—</span>", "This is not your card",
                        "id=\"booknet-navbar\"")
                .doesNotContain("th:field", "Member management</a>", "Authorization</a>");
        write("account", account);
        write("reset-expired", mvc.perform(get("/reset-password")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
    }

    private User save(String name, String email, UserRole role) {
        return users.saveAndFlush(User.builder().fullName(name).email(email).role(role).provider(AuthProvider.LOCAL)
                .status(UserStatus.ACTIVE).enabled(true).emailVerified(true).build());
    }

    private void write(String name, String html) throws Exception {
        Path output = Path.of("target", "ui-review");
        Files.createDirectories(output);
        String assets = Path.of("src/main/resources/static").toAbsolutePath().toUri().toString();
        Files.writeString(output.resolve(name + ".html"), html.replace("href=\"/css/", "href=\"" + assets + "css/")
                .replace("src=\"/images/", "src=\"" + assets + "images/")
                .replace("src=\"/js/", "src=\"" + assets + "js/"));
    }
}
