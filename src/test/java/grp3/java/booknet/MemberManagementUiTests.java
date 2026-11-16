package grp3.java.booknet;

import grp3.java.booknet.entity.user.User;
import grp3.java.booknet.enums.user.*;
import grp3.java.booknet.repository.user.*;
import grp3.java.booknet.security.CustomUserDetails;
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
    @MockitoBean JavaMailSender sender;
    User librarian;
    User admin;
    User member;

    @BeforeEach
    void prepare() {
        tokens.deleteAll();
        identities.deleteAll();
        users.deleteAll();
        librarian = save("Library Staff", "staff@example.com", UserRole.ROLE_LIBRARIAN);
        admin = save("Library Admin", "admin@example.com", UserRole.ROLE_ADMIN);
        member = save("Actual Reader", "reader@example.com", UserRole.ROLE_USER);
    }

    @Test
    void liveDirectoryUsesRealMembersAndNeverFillsMissingValuesWithDemoData() throws Exception {
        String html = mvc.perform(get("/staff/members").with(user(new CustomUserDetails(librarian))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(html).contains("Actual Reader", "LIVE DIRECTORY", "Not connected", "Showing 1–1 of 1 records")
                .doesNotContain("Nguyễn Minh Anh", "TV-02481", "Library Admin", "th:each");
        mvc.perform(get("/staff/members").param("keyword", "nothing").with(user(new CustomUserDetails(librarian))))
                .andExpect(content().string(containsString("No matching records")));
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
    void liveFiltersDoNotPretendToBeConnectedAndSearchIsEscaped() throws Exception {
        var signedIn = user(new CustomUserDetails(librarian));
        mvc.perform(get("/staff/members").param("loan", "overdue").with(signedIn)).andExpect(status().isBadRequest());
        mvc.perform(get("/staff/members").param("demo", "true").param("card", "unknown").with(signedIn))
                .andExpect(status().isBadRequest());
        String html = mvc.perform(get("/staff/members").param("demo", "true").param("keyword", "<script>alert(1)</script>").with(signedIn))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(html).doesNotContain("<script>alert(1)</script>").contains("&lt;script&gt;");
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
        assertThat(adminHtml).contains("Save role", "Lock / unlock account", "disabled", "Preview only", "Actual Reader");
        mvc.perform(post("/admin/users/" + member.getId()).with(user(new CustomUserDetails(admin))).with(csrf())
                        .param("role", "ROLE_ADMIN")).andExpect(status().isMethodNotAllowed());
        assertThat(users.findById(member.getId()).orElseThrow().getRole()).isEqualTo(UserRole.ROLE_USER);
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
        assertThat(account).contains("name=\"fullName\"", "name=\"_csrf\"", "Card information not connected", "This is not your card")
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
