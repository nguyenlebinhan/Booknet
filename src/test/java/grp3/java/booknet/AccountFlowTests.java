package grp3.java.booknet;

import grp3.java.booknet.dto.AccountRequests;
import grp3.java.booknet.entity.user.AccountToken;
import grp3.java.booknet.entity.user.OAuthIdentity;
import grp3.java.booknet.entity.user.User;
import grp3.java.booknet.enums.user.*;
import grp3.java.booknet.exception.AccountException;
import grp3.java.booknet.repository.user.*;
import grp3.java.booknet.security.*;
import grp3.java.booknet.service.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.RememberMeAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AccountFlowTests {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired AccountTokenRepository tokens;
    @Autowired OAuthIdentityRepository identities;
    @Autowired PasswordEncoder passwords;
    @Autowired AccountService accounts;
    @Autowired OAuthAccountService oauthAccounts;
    @Autowired ClientRegistrationRepository clients;
    @MockitoBean JavaMailSender sender;
    private MockHttpServletRequest oauthRequest;

    @BeforeEach
    void prepare() {
        tokens.deleteAll();
        identities.deleteAll();
        users.deleteAll();
        reset(sender);
        SecurityContextHolder.clearContext();
        oauthRequest = new MockHttpServletRequest();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(oauthRequest));
    }

    @AfterEach
    void cleanup() {
        SecurityContextHolder.clearContext();
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void registrationRequiresVerificationThenAllowsNormalizedEmailLogin() throws Exception {
        register("Reader@Example.com", "Secret123", true).andExpect(redirectedUrl("/login?registered=true"));
        User user = users.findByEmail("reader@example.com").orElseThrow();
        assertThat(user.isEmailVerified()).isFalse();
        assertThat(user.isEnabled()).isTrue();
        assertThat(passwords.matches("Secret123", user.getPassword())).isTrue();
        assertThat(user.getRole()).isEqualTo(UserRole.ROLE_USER);
        mvc.perform(post("/login").with(csrf()).param("email", user.getEmail()).param("password", "Secret123"))
                .andExpect(redirectedUrl("/login?error=true"));
        String token = deliveredToken();
        assertThat(tokens.findAll().getFirst().getTokenHash()).isNotEqualTo(token).hasSize(64);
        mvc.perform(get("/verify-email").param("token", token)).andExpect(redirectedUrl("/login"))
                .andExpect(flash().attributeExists("successMessage"));
        mvc.perform(get("/verify-email").param("token", token)).andExpect(redirectedUrl("/login"))
                .andExpect(flash().attributeExists("errorMessage"));
        login("  READER@EXAMPLE.COM  ", "Secret123");
    }

    @Test
    void registrationValidatesConfirmationTermsLengthAndDuplicateEmail() throws Exception {
        register("reader@example.com", "Different123", true)
                .andExpect(view().name("register")).andExpect(model().attributeHasFieldErrors("user", "confirmPassword"));
        register("reader@example.com", "Secret123", false)
                .andExpect(view().name("register")).andExpect(model().attributeHasFieldErrors("user", "termsAccepted"));
        localUser("reader@example.com", true);
        register("READER@example.com", "Secret123", true)
                .andExpect(view().name("register")).andExpect(model().attributeHasFieldErrors("user", "email"));
        verifyNoInteractions(sender);
    }

    @Test
    void failedRegistrationMailRollsBackUserAndToken() throws Exception {
        doThrow(new MailSendException("SMTP unavailable")).when(sender).send(any(SimpleMailMessage.class));
        register("reader@example.com", "Secret123", true).andExpect(view().name("register"))
                .andExpect(model().attributeExists("globalError"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Unable to send the verification email")));
        assertThat(users.count()).isZero();
        assertThat(tokens.count()).isZero();
    }

    @Test
    void verificationExpiresAndResendInvalidatesPriorLinkWithCooldown() throws Exception {
        localUser("reader@example.com", false);
        requestEmail("/verification/resend", "reader@example.com");
        String original = deliveredToken();
        requestEmail("/verification/resend", "reader@example.com");
        verify(sender, times(1)).send(any(SimpleMailMessage.class));
        AccountToken token = tokens.findAll().getFirst();
        token.setIssuedAt(Instant.now().minusSeconds(120));
        tokens.saveAndFlush(token);
        requestEmail("/verification/resend", "reader@example.com");
        String replacement = deliveredToken();
        assertThat(replacement).isNotEqualTo(original);
        mvc.perform(get("/verify-email").param("token", original)).andExpect(flash().attributeExists("errorMessage"));
        AccountToken latest = tokens.findAll().stream().filter(t -> t.getUsedAt() == null).findFirst().orElseThrow();
        latest.setExpiresAt(Instant.now().minusSeconds(1));
        tokens.saveAndFlush(latest);
        mvc.perform(get("/verify-email").param("token", replacement)).andExpect(flash().attributeExists("errorMessage"));
        assertThat(users.findByEmail("reader@example.com").orElseThrow().isEmailVerified()).isFalse();
    }

    @Test
    void forgotPasswordDoesNotExposeUnknownGoogleOrUnverifiedAccounts() throws Exception {
        localUser("reader@example.com", true);
        localUser("unverified@example.com", false);
        User google = localUser("google@example.com", true);
        google.setProvider(AuthProvider.GOOGLE);
        google.setPassword(null);
        users.saveAndFlush(google);
        String known = requestEmail("/forgot-password", "reader@example.com");
        assertThat(requestEmail("/forgot-password", "missing@example.com")).isEqualTo(known);
        assertThat(requestEmail("/forgot-password", "google@example.com")).isEqualTo(known);
        assertThat(requestEmail("/forgot-password", "unverified@example.com")).isEqualTo(known);
        requestEmail("/forgot-password", "reader@example.com");
        verify(sender, times(1)).send(any(SimpleMailMessage.class));
    }

    @Test
    void smtpFailureDuringForgotStillReturnsGenericResponse() throws Exception {
        localUser("reader@example.com", true);
        doThrow(new MailSendException("SMTP unavailable")).when(sender).send(any(SimpleMailMessage.class));
        String known = requestEmail("/forgot-password", "reader@example.com");
        assertThat(requestEmail("/forgot-password", "missing@example.com")).isEqualTo(known);
        assertThat(tokens.findAll()).allMatch(token -> token.getUsedAt() != null);
    }

    @Test
    void resetPasswordIsSingleUseAndRevokesLocalGoogleAndRememberMeSessions() throws Exception {
        User user = localUser("reader@example.com", true);
        MockHttpSession previous = login(user.getEmail(), "Secret123");
        var remembered = mvc.perform(post("/login").with(csrf()).param("email", user.getEmail())
                .param("password", "Secret123").param("remember-me", "on"))
                .andExpect(redirectedUrl("/")).andReturn().getResponse().getCookie("remember-me");
        CustomOAuth2User googlePrincipal = new CustomOAuth2User(google("google-sub", "google@example.com", true), user);
        requestEmail("/forgot-password", user.getEmail());
        String token = deliveredToken();
        mvc.perform(get("/reset-password").param("token", token)).andExpect(model().attribute("tokenValid", true));
        resetPassword(token, "NewSecret123", "Mismatch123").andExpect(view().name("reset-password"))
                .andExpect(model().attributeExists("confirmPasswordError"));
        resetPassword(token, "NewSecret123", "NewSecret123").andExpect(redirectedUrl("/login?reset=true"));
        resetPassword(token, "Another123", "Another123").andExpect(model().attribute("tokenValid", false));
        mvc.perform(get("/account/profile").session(previous)).andExpect(redirectedUrl("/login?error=true"));
        mvc.perform(get("/account/profile").with(oauth2Login().oauth2User(googlePrincipal)))
                .andExpect(redirectedUrl("/login?error=true"));
        assertThat(remembered).isNotNull();
        mvc.perform(get("/account/profile").cookie(remembered)).andExpect(redirectedUrl("/login"));
        mvc.perform(post("/login").with(csrf()).param("email", user.getEmail()).param("password", "Secret123"))
                .andExpect(redirectedUrl("/login?error=true"));
        login(user.getEmail(), "NewSecret123");
    }

    @Test
    void resetRejectsExpiredWrongPurposeAndInvalidTokens() throws Exception {
        User user = localUser("reader@example.com", false);
        requestEmail("/verification/resend", user.getEmail());
        String verification = deliveredToken();
        resetPassword(verification, "NewSecret123", "NewSecret123").andExpect(model().attribute("tokenValid", false));
        mvc.perform(get("/verify-email").param("token", verification)).andExpect(flash().attributeExists("successMessage"));
        requestEmail("/forgot-password", user.getEmail());
        String reset = deliveredToken();
        mvc.perform(get("/verify-email").param("token", reset)).andExpect(flash().attributeExists("errorMessage"));
        AccountToken token = tokens.findAll().stream().filter(t -> t.getPurpose() == AccountTokenPurpose.RESET_PASSWORD)
                .findFirst().orElseThrow();
        token.setExpiresAt(Instant.now().minusSeconds(1));
        tokens.saveAndFlush(token);
        resetPassword(reset, "NewSecret123", "NewSecret123").andExpect(model().attribute("tokenValid", false));
        mvc.perform(get("/reset-password").param("token", "invalid")).andExpect(model().attribute("tokenValid", false));
        assertThat(passwords.matches("Secret123", users.findById(user.getId()).orElseThrow().getPassword())).isTrue();
    }

    @Test
    void concurrentResetConsumesTokenOnlyOnce() throws Exception {
        User user = localUser("reader@example.com", true);
        requestEmail("/forgot-password", user.getEmail());
        var request = new AccountRequests.ResetPassword(deliveredToken(), "NewSecret123", "NewSecret123");
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        Callable<Boolean> attempt = () -> {
            start.await();
            try { accounts.resetPassword(request); return true; }
            catch (AccountException ex) { return false; }
        };
        try {
            Future<Boolean> first = executor.submit(attempt);
            Future<Boolean> second = executor.submit(attempt);
            start.countDown();
            assertThat(List.of(first.get(15, TimeUnit.SECONDS), second.get(15, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(true, false);
            assertThat(users.findById(user.getId()).orElseThrow().getAuthVersion()).isEqualTo(1);
        } finally { executor.shutdownNow(); }
    }

    @Test
    void profileIsPrivateAndOnlyAllowedFieldsCanBeUpdated() throws Exception {
        User user = localUser("reader@example.com", true);
        mvc.perform(get("/account/profile")).andExpect(redirectedUrl("/login"));
        MockHttpSession session = login(user.getEmail(), "Secret123");
        mvc.perform(get("/account/profile").session(session)).andExpect(status().isOk()).andExpect(view().name("profile"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML));
        mvc.perform(post("/account/profile").session(session).with(csrf())
                .param("fullName", "  Updated Reader  ").param("phone", "+84 123456789").param("address", "HCM")
                .param("avatarUrl", "https://example.com/avatar.png").param("role", "ROLE_ADMIN")
                .param("email", "other@example.com").param("id", "999").param("password", "Injected123")
                .param("provider", "GOOGLE").param("status", "SUSPENDED").param("memberCode", "injected"))
                .andExpect(redirectedUrl("/account/profile")).andExpect(flash().attributeExists("successMessage"));
        User updated = users.findById(user.getId()).orElseThrow();
        assertThat(updated.getFullName()).isEqualTo("Updated Reader");
        assertThat(updated.getEmail()).isEqualTo(user.getEmail());
        assertThat(updated.getRole()).isEqualTo(UserRole.ROLE_USER);
        assertThat(updated.getProvider()).isEqualTo(AuthProvider.LOCAL);
        assertThat(updated.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(updated.getMemberCode()).isNull();
        assertThat(updated.getPassword()).isEqualTo(user.getPassword());
        mvc.perform(post("/account/profile").session(session).with(csrf())
                .param("fullName", "Updated Reader").param("avatarUrl", "javascript:alert(1)"))
                .andExpect(view().name("profile")).andExpect(model().attributeHasFieldErrors("profileForm", "avatarUrl"));
    }

    @Test
    void changePasswordRequiresCurrentPasswordAndLogsOutOtherSessions() throws Exception {
        User user = localUser("reader@example.com", true);
        MockHttpSession session = login(user.getEmail(), "Secret123");
        MockHttpSession other = login(user.getEmail(), "Secret123");
        changePassword(session, "wrong", "NewSecret123").andExpect(view().name("change-password"))
                .andExpect(model().attributeHasErrors("passwordForm"));
        changePassword(session, "Secret123", "Secret123").andExpect(view().name("change-password"))
                .andExpect(model().attributeHasErrors("passwordForm"));
        changePassword(session, "Secret123", "NewSecret123").andExpect(redirectedUrl("/login"))
                .andExpect(flash().attributeExists("successMessage"));
        assertThat(session.isInvalid()).isTrue();
        mvc.perform(get("/account/profile").session(other)).andExpect(redirectedUrl("/login?error=true"));
        login(user.getEmail(), "NewSecret123");
    }

    @Test
    void csrfIsRequiredAndRenderedFormsContainToken() throws Exception {
        mvc.perform(post("/forgot-password").param("email", "reader@example.com")).andExpect(status().isForbidden());
        mvc.perform(get("/login")).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"_csrf\"")));
    }

    @ParameterizedTest
    @EnumSource(value = UserStatus.class, names = {"SUSPENDED", "EXPIRED"})
    void inactiveAccountsCannotSignInLocallyOrViaLinkedGoogle(UserStatus status) throws Exception {
        User user = localUser("reader@example.com", true);
        user.setStatus(status);
        user = users.saveAndFlush(user);
        identity(user, "blocked-google");
        mvc.perform(post("/login").with(csrf()).param("email", user.getEmail()).param("password", "Secret123"))
                .andExpect(redirectedUrl("/login?error=true"));
        assertThatThrownBy(() -> oauthAccounts.authenticate(google("blocked-google", "reader@example.com", true)))
                .isInstanceOf(OAuth2AuthenticationException.class);
    }

    @Test
    void disablingUserRevokesExistingSessionAndBlocksGoogle() throws Exception {
        User user = localUser("reader@example.com", true);
        MockHttpSession session = login(user.getEmail(), "Secret123");
        user.setEnabled(false);
        users.saveAndFlush(user);
        identity(user, "disabled-google");
        mvc.perform(get("/account/profile").session(session)).andExpect(redirectedUrl("/login?error=true"));
        assertThatThrownBy(() -> oauthAccounts.authenticate(google("disabled-google", user.getEmail(), true)))
                .isInstanceOf(OAuth2AuthenticationException.class);
    }

    @Test
    void googleCreatesVerifiedAccountAndUsesStableSubjectWithoutAutoMergingLocalAccount() {
        User first = oauthAccounts.authenticate(google("new-google", "google@example.com", true));
        assertThat(first.getProvider()).isEqualTo(AuthProvider.GOOGLE);
        assertThat(first.isEmailVerified()).isTrue();
        assertThat(first.getPassword()).isNull();
        User again = oauthAccounts.authenticate(google("new-google", "changed@example.com", true));
        assertThat(again.getId()).isEqualTo(first.getId());
        assertThat(users.count()).isEqualTo(1);
        localUser("local@example.com", true);
        assertThatThrownBy(() -> oauthAccounts.authenticate(google("unlinked-google", "local@example.com", true)))
                .isInstanceOf(OAuth2AuthenticationException.class);
        assertThat(identities.count()).isEqualTo(1);
        assertThatThrownBy(() -> oauthAccounts.authenticate(google("unverified-google", "new@example.com", false)))
                .isInstanceOf(OAuth2AuthenticationException.class);
    }

    @Test
    void explicitLinkKeepsLocalCredentialsAndGoogleLogsIntoSameAccount() {
        User user = localUser("local@example.com", true);
        beginLink(user);
        String originalPassword = user.getPassword();
        User linked = oauthAccounts.authenticate(google("linked-google", "different@example.com", true));
        assertThat(linked.getId()).isEqualTo(user.getId());
        assertThat(linked.getProvider()).isEqualTo(AuthProvider.LOCAL);
        assertThat(linked.getPassword()).isEqualTo(originalPassword);
        assertThat(oauthRequest.getSession().getAttribute(OAuthLinkSessionAttributes.GOOGLE_LINK_SUCCEEDED)).isEqualTo(true);
        // A retry of the same link is idempotent.
        assertThat(oauthAccounts.authenticate(google("linked-google", "different@example.com", true)).getId()).isEqualTo(user.getId());
        SecurityContextHolder.clearContext();
        oauthRequest.getSession().invalidate();
        assertThat(oauthAccounts.authenticate(google("linked-google", "different@example.com", true)).getId()).isEqualTo(user.getId());
        assertThat(identities.count()).isEqualTo(1);
    }

    @Test
    void linkRejectsOtherOwnerSecondGoogleExpiredMismatchedAndStaleSessions() {
        User first = localUser("first@example.com", true);
        User second = localUser("second@example.com", true);
        identity(second, "owned-google");
        beginLink(first);
        assertThatThrownBy(() -> oauthAccounts.authenticate(google("owned-google", "second@example.com", true)))
                .isInstanceOf(OAuth2AuthenticationException.class);
        identity(first, "first-google");
        assertThatThrownBy(() -> oauthAccounts.authenticate(google("second-google", "third@example.com", true)))
                .isInstanceOf(OAuth2AuthenticationException.class);
        oauthRequest.getSession().setAttribute(OAuthLinkSessionAttributes.GOOGLE_LINK_STARTED_AT,
                System.currentTimeMillis() - OAuthLinkSessionAttributes.GOOGLE_LINK_MAX_AGE_MILLIS - 1000);
        assertThatThrownBy(() -> oauthAccounts.authenticate(google("first-google", "first@example.com", true)))
                .isInstanceOf(OAuth2AuthenticationException.class);
        beginLink(first);
        oauthRequest.getSession().setAttribute(OAuthLinkSessionAttributes.GOOGLE_LINK_USER_ID, second.getId());
        assertThatThrownBy(() -> oauthAccounts.authenticate(google("first-google", "first@example.com", true)))
                .isInstanceOf(OAuth2AuthenticationException.class);
        beginLink(first);
        first.setAuthVersion(1);
        users.saveAndFlush(first);
        assertThatThrownBy(() -> oauthAccounts.authenticate(google("first-google", "first@example.com", true)))
                .isInstanceOf(OAuth2AuthenticationException.class);
    }

    @Test
    void linkRequiresPasswordLoginRatherThanRememberMe() throws Exception {
        User user = localUser("reader@example.com", true);
        var remembered = new RememberMeAuthenticationToken("test-key", new CustomUserDetails(user),
                List.of(new SimpleGrantedAuthority("ROLE_USER")));
        mvc.perform(post("/account/link/google").with(authentication(remembered)).with(csrf()))
                .andExpect(redirectedUrl("/")).andExpect(flash().attributeExists("googleLinkError"));
        beginLink(user);
        SecurityContextHolder.getContext().setAuthentication(remembered);
        assertThatThrownBy(() -> oauthAccounts.authenticate(google("new-google", "reader@example.com", true)))
                .isInstanceOf(OAuth2AuthenticationException.class);
    }

    @Test
    void oidcAdapterRunsBooknetAccountLogicAndPreservesIdTokenAndRole() {
        CustomOidcUserService service = new CustomOidcUserService(oauthAccounts);
        service.setRetrieveUserInfo(request -> false); // No external Google calls in this test.
        Instant now = Instant.now();
        OidcIdToken idToken = new OidcIdToken("test-id-token", now, now.plusSeconds(300),
                Map.of("sub", "oidc-google", "email", "oidc@example.com", "email_verified", true, "name", "OIDC Reader"));
        OAuth2AccessToken accessToken = new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER,
                "test-access-token", now, now.plusSeconds(300), Set.of("openid", "profile", "email"));
        var principal = service.loadUser(new OidcUserRequest(clients.findByRegistrationId("google"), accessToken, idToken));
        assertThat(principal).isInstanceOf(CustomOidcUser.class);
        assertThat(principal.getIdToken()).isSameAs(idToken);
        assertThat(principal.getAuthorities()).extracting("authority").containsExactly("ROLE_USER");
        assertThat(users.findByEmail("oidc@example.com")).isPresent();
        localUser("local@example.com", true);
        var localToken = new OidcIdToken("local-id-token", now, now.plusSeconds(300),
                Map.of("sub", "local-oidc", "email", "local@example.com", "email_verified", true));
        assertThatThrownBy(() -> service.loadUser(new OidcUserRequest(clients.findByRegistrationId("google"), accessToken, localToken)))
                .isInstanceOf(OAuth2AuthenticationException.class);
    }

    private User localUser(String email, boolean verified) {
        return users.saveAndFlush(User.builder().fullName("Test Reader").email(email).username("reader")
                .password(passwords.encode("Secret123")).provider(AuthProvider.LOCAL).role(UserRole.ROLE_USER)
                .enabled(true).emailVerified(verified).status(UserStatus.ACTIVE).build());
    }

    private MockHttpSession login(String email, String password) throws Exception {
        return (MockHttpSession) mvc.perform(post("/login").with(csrf()).param("email", email).param("password", password))
                .andExpect(redirectedUrl("/")).andReturn().getRequest().getSession(false);
    }

    private String requestEmail(String path, String email) throws Exception {
        var result = mvc.perform(post(path).with(csrf()).param("email", email))
                .andExpect(redirectedUrl(path)).andExpect(flash().attribute("requestSubmitted", true)).andReturn();
        return result.getFlashMap().get("requestSubmitted").toString();
    }

    private org.springframework.test.web.servlet.ResultActions register(String email, String confirmation, boolean terms) throws Exception {
        return mvc.perform(post("/register").with(csrf()).param("fullName", "Test Reader").param("email", email)
                .param("phone", "0912345678").param("password", "Secret123").param("confirmPassword", confirmation)
                .param("termsAccepted", Boolean.toString(terms)));
    }

    private org.springframework.test.web.servlet.ResultActions resetPassword(String token, String password, String confirmation) throws Exception {
        return mvc.perform(post("/reset-password").with(csrf()).param("token", token)
                .param("password", password).param("confirmPassword", confirmation));
    }

    private org.springframework.test.web.servlet.ResultActions changePassword(MockHttpSession session, String current, String password) throws Exception {
        return mvc.perform(post("/account/change-password").session(session).with(csrf()).param("currentPassword", current)
                .param("newPassword", password).param("confirmPassword", password));
    }

    private String deliveredToken() {
        var messages = mockingDetails(sender).getInvocations().stream()
                .filter(invocation -> invocation.getArgument(0) instanceof SimpleMailMessage)
                .map(invocation -> (SimpleMailMessage) invocation.getArgument(0)).toList();
        String text = messages.getLast().getText();
        var matcher = Pattern.compile("[?&]token=([A-Za-z0-9_-]+)").matcher(text);
        assertThat(matcher.find()).isTrue();
        return matcher.group(1);
    }

    private OAuth2User google(String sub, String email, boolean verified) {
        return new DefaultOAuth2User(List.of(new SimpleGrantedAuthority("OAUTH2_USER")),
                Map.of("sub", sub, "email", email, "email_verified", verified, "name", "Google Reader"), "sub");
    }

    private void identity(User user, String sub) {
        identities.saveAndFlush(OAuthIdentity.builder().user(user).provider(AuthProvider.GOOGLE)
                .providerUserId(sub).providerSubject(sub).email(user.getEmail()).build());
    }

    private void beginLink(User user) {
        CustomUserDetails principal = new CustomUserDetails(user);
        SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                principal, null, principal.getAuthorities()));
        oauthRequest.getSession().setAttribute(OAuthLinkSessionAttributes.GOOGLE_LINK_USER_ID, user.getId());
        oauthRequest.getSession().setAttribute(OAuthLinkSessionAttributes.GOOGLE_LINK_STARTED_AT, System.currentTimeMillis());
        oauthRequest.getSession().removeAttribute(OAuthLinkSessionAttributes.GOOGLE_LINK_SUCCEEDED);
    }
}
