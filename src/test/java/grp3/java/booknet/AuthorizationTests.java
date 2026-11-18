package grp3.java.booknet;

import grp3.java.booknet.entity.user.User;
import grp3.java.booknet.enums.user.*;
import grp3.java.booknet.repository.user.*;
import grp3.java.booknet.security.dto.CustomUserDetails;
import grp3.java.booknet.security.principal.CustomOAuth2User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(AuthorizationTests.PermissionProbe.class)
class AuthorizationTests {
    private static final String PASSWORD = TestPasswords.valid();
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired AccountTokenRepository tokens;
    @Autowired OAuthIdentityRepository identities;
    @Autowired PasswordEncoder passwords;
    @MockitoBean JavaMailSender sender;

    @BeforeEach
    void prepare() {
        tokens.deleteAll();
        identities.deleteAll();
        users.deleteAll();
    }

    @Test
    void guestCanReadPublicHtmlButCannotReadPrivatePagesOrApis() throws Exception {
        mvc.perform(get("/about")).andExpect(status().isOk());
        for (String path : new String[]{"/account/profile", "/staff/probe", "/admin/probe", "/member/probe"}) {
            mvc.perform(get(path)).andExpect(redirectedUrl("/login"));
        }
        for (String path : new String[]{"/api/staff/probe", "/api/admin/probe", "/api/member/probe"}) {
            mvc.perform(get(path)).andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error").value("Authentication required"));
        }
    }

    @ParameterizedTest
    @EnumSource(value = UserRole.class, names = "ROLE_GUEST", mode = EnumSource.Mode.EXCLUDE)
    void realLocalLoginEnforcesPageAndApiRoleMatrix(UserRole role) throws Exception {
        User account = account(role);
        MockHttpSession session = login(account);
        mvc.perform(get("/account/profile").session(session)).andExpect(status().isOk())
                .andExpect(view().name("account/profile"));
        checkMatrix(request -> { request.setSession(session); return request; }, role);
    }

    @ParameterizedTest
    @EnumSource(value = UserRole.class, names = "ROLE_GUEST", mode = EnumSource.Mode.EXCLUDE)
    void googlePrincipalHasSameRoleAndPermissionsAsLocal(UserRole role) throws Exception {
        User account = account(role);
        CustomOAuth2User google = google(account);
        assertThat(google.getAuthorities()).isEqualTo(new CustomUserDetails(account).getAuthorities());
        checkMatrix(authentication(new OAuth2AuthenticationToken(google, google.getAuthorities(), "google")), role);
    }

    @ParameterizedTest
    @EnumSource(value = UserRole.class, names = "ROLE_GUEST", mode = EnumSource.Mode.EXCLUDE)
    void methodPermissionsAndCsrfProtectMutations(UserRole role) throws Exception {
        var signedIn = user(new CustomUserDetails(account(role)));
        mvc.perform(post("/api/staff/probe/activate").with(signedIn)).andExpect(status().isForbidden());
        mvc.perform(post("/api/staff/probe/activate").with(signedIn).with(csrf()))
                .andExpect(status().is(role == UserRole.ROLE_USER ? 403 : 200));
        // A staff URL does not grant Librarian the Admin-only method permission.
        mvc.perform(post("/api/staff/probe/role").with(signedIn).with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    void storedGuestCannotAccessAccountEvenWhenAuthenticated() throws Exception {
        mvc.perform(get("/account/profile").with(user(new CustomUserDetails(account(UserRole.ROLE_GUEST)))))
                .andExpect(status().isForbidden());
    }

    @Test
    void changingRoleRevokesExistingLocalAndGoogleSessionsWithoutWaitingForVersionUpdate() throws Exception {
        User admin = account(UserRole.ROLE_ADMIN);
        MockHttpSession session = login(admin);
        CustomOAuth2User google = google(admin);
        admin.setRole(UserRole.ROLE_USER);
        users.saveAndFlush(admin);
        mvc.perform(get("/admin/probe").session(session)).andExpect(redirectedUrl("/login?error=true"));
        mvc.perform(get("/api/admin/probe").with(authentication(
                        new OAuth2AuthenticationToken(google, google.getAuthorities(), "google"))))
                .andExpect(status().isUnauthorized()).andExpect(header().doesNotExist("Location"));
        mvc.perform(get("/admin/probe").session(login(admin))).andExpect(status().isForbidden());
    }

    @Test
    void accountSessionFilterRejectsStaleAuthoritiesAndAllowsOnlyCurrentRoleAuthorities() throws Exception {
        User admin = account(UserRole.ROLE_ADMIN);
        MockHttpSession session = login(admin);
        admin.setRole(UserRole.ROLE_LIBRARIAN);
        users.saveAndFlush(admin);
        mvc.perform(get("/staff/probe").session(session)).andExpect(redirectedUrl("/login?error=true"));
        assertThat(session.isInvalid()).isTrue();
    }

    @Test
    void rememberMeReloadsCurrentRoleAndDisabledApiSessionReturns401() throws Exception {
        User admin = account(UserRole.ROLE_ADMIN);
        var remembered = mvc.perform(post("/login").with(csrf()).param("email", admin.getEmail())
                        .param("password", PASSWORD).param("remember-me", "on"))
                .andExpect(redirectedUrl("/admin/dashboard")).andReturn().getResponse().getCookie("remember-me");
        assertThat(remembered).isNotNull();
        assertThat(remembered.getMaxAge()).isEqualTo(30 * 24 * 60 * 60);
        admin.setRole(UserRole.ROLE_USER);
        users.saveAndFlush(admin);
        mvc.perform(get("/api/member/probe").cookie(remembered)).andExpect(status().isOk());
        // Spring requests full sign-in when a remember-me authentication lacks the required role.
        mvc.perform(get("/api/admin/probe").cookie(remembered)).andExpect(status().isUnauthorized());
        MockHttpSession session = login(admin);
        admin.setEnabled(false);
        users.saveAndFlush(admin);
        mvc.perform(get("/api/member/probe").session(session)).andExpect(status().isUnauthorized());
    }

    @Test
    void loginRedirectsEachRoleToItsOwnArea() throws Exception {
        for (UserRole role : new UserRole[]{UserRole.ROLE_USER, UserRole.ROLE_LIBRARIAN, UserRole.ROLE_ADMIN}) {
            User account = account(role);
            String expected = switch (role) {
                case ROLE_USER -> "/account/profile";
                case ROLE_LIBRARIAN -> "/staff/members";
                case ROLE_ADMIN -> "/admin/dashboard";
                case ROLE_GUEST -> "/";
            };
            mvc.perform(post("/login").with(csrf()).param("email", account.getEmail()).param("password", PASSWORD))
                    .andExpect(redirectedUrl(expected));
            users.delete(account);
        }
    }

    @ParameterizedTest
    @EnumSource(value = UserRole.class, names = "ROLE_GUEST", mode = EnumSource.Mode.EXCLUDE)
    void membersGetTheNavbarAndStaffGetTheSidebarWithALogoutForm(UserRole role) throws Exception {
        String html = mvc.perform(get("/about").with(user(new CustomUserDetails(account(role)))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(html).contains("action=\"/logout\"", "name=\"_csrf\"")
                .doesNotContain("sec:authorize");
        if (role == UserRole.ROLE_USER) assertThat(html).contains("My books", ">Reservations<");
        else assertThat(html).doesNotContain("My books", "href=\"#\" class=\"bn-nav-link\">Reservations");
    }

    private void checkMatrix(RequestPostProcessor signedIn, UserRole role) throws Exception {
        for (String prefix : new String[]{"", "/api"}) {
            mvc.perform(get(prefix + "/admin/probe").with(signedIn))
                    .andExpect(status().is(role == UserRole.ROLE_ADMIN ? 200 : 403));
            mvc.perform(get(prefix + "/staff/probe").with(signedIn))
                    .andExpect(status().is(role == UserRole.ROLE_USER ? 403 : 200));
            mvc.perform(get(prefix + "/member/probe").with(signedIn))
                    .andExpect(status().is(role == UserRole.ROLE_USER ? 200 : 403));
        }
    }

    private User account(UserRole role) {
        return users.saveAndFlush(User.builder().fullName("Authorization Reader").email("reader@example.com")
                .password(passwords.encode(PASSWORD)).provider(AuthProvider.LOCAL).role(role)
                .enabled(true).emailVerified(true).status(UserStatus.ACTIVE).build());
    }

    private MockHttpSession login(User account) throws Exception {
        String destination = account.getRole() == UserRole.ROLE_ADMIN ? "/admin/dashboard" : account.getRole() == UserRole.ROLE_LIBRARIAN ? "/staff/members" : "/account/profile";
        return (MockHttpSession) mvc.perform(post("/login").with(csrf()).param("email", account.getEmail())
                        .param("password", PASSWORD)).andExpect(redirectedUrl(destination))
                .andReturn().getRequest().getSession(false);
    }

    /** Admins land on their dashboard after sign-in; everyone else lands on the home page. */
    private static String landing(User account) {
        return account.getRole() == UserRole.ROLE_ADMIN ? "/admin/dashboard" : "/";
    }

    private CustomOAuth2User google(User account) {
        return new CustomOAuth2User(new DefaultOAuth2User(account.getRole().authorities(),
                Map.of("sub", "test-subject", "email", account.getEmail()), "sub"), account);
    }

    /** Test-only handlers exercise the real filter chain without adding unfinished production endpoints. */
    @RestController
    static class PermissionProbe {
        @GetMapping({"/admin/probe", "/staff/probe", "/member/probe",
                "/api/admin/probe", "/api/staff/probe", "/api/member/probe"})
        String read() { return "ok"; }

        @PostMapping("/api/staff/probe/activate")
        @PreAuthorize("hasAuthority('MEMBER_CARD_ACTIVATE')")
        public String activate() { return "ok"; }

        @PostMapping("/api/staff/probe/role")
        @PreAuthorize("hasAuthority('USER_ROLE_UPDATE')")
        public String changeRole() { return "ok"; }
    }
}
