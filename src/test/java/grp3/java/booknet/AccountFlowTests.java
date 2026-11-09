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
    private static final String INITIAL_PASSWORD = TestPasswords.valid();
    private static final String NEW_PASSWORD = TestPasswords.valid();
    private static final String OTHER_PASSWORD = TestPasswords.valid();

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
        mvc.perform(post("/api/auth/register").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"fullName":"Test Reader","email":"Reader@Example.com","password":"%s",
                         "confirmPassword":"%s","phone":"0912345678","termsAccepted":true}
                        """.formatted(INITIAL_PASSWORD, INITIAL_PASSWORD))).andExpect(status().isCreated());
        User user = users.findByEmail("reader@example.com").orElseThrow();
        assertThat(user.isEmailVerified()).isFalse();
        assertThat(user.isEnabled()).isTrue();
        assertThat(passwords.matches(INITIAL_PASSWORD, user.getPassword())).isTrue();
        assertThat(user.getRole()).isEqualTo(UserRole.ROLE_USER);
        mvc.perform(post("/login").with(csrf()).param("email", user.getEmail()).param("password", INITIAL_PASSWORD))
                .andExpect(redirectedUrl("/login?error=true"));
        String token = deliveredToken();
        assertThat(tokens.findAll().getFirst().getTokenHash()).isNotEqualTo(token).hasSize(64);
        mvc.perform(get("/api/auth/verify-email").param("token", token)).andExpect(status().isOk());
        mvc.perform(get("/api/auth/verify-email").param("token", token)).andExpect(status().isBadRequest());
        login("  READER@EXAMPLE.COM  ", INITIAL_PASSWORD);
    }

    @Test
    void registrationValidatesConfirmationTermsLengthAndDuplicateEmail() throws Exception {
        mvc.perform(post("/api/auth/register").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"fullName":"Test Reader","email":"reader@example.com","password":"%s",
                         "confirmPassword":"%s","phone":"0912345678","termsAccepted":true}
                        """.formatted(INITIAL_PASSWORD, OTHER_PASSWORD))).andExpect(status().isBadRequest());
        mvc.perform(post("/api/auth/register").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"fullName":"Test Reader","email":"reader@example.com","password":"%s",
                         "confirmPassword":"%s","phone":"0912345678","termsAccepted":false}
                        """.formatted(INITIAL_PASSWORD, INITIAL_PASSWORD))).andExpect(status().isBadRequest());
        localUser("reader@example.com", true);
        mvc.perform(post("/api/auth/register").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"fullName":"Test Reader","email":"READER@example.com","password":"%s",
                         "confirmPassword":"%s","phone":"0912345678","termsAccepted":true}
                        """.formatted(INITIAL_PASSWORD, INITIAL_PASSWORD))).andExpect(status().isConflict());
        verifyNoInteractions(sender);
    }

    @Test
    void failedRegistrationMailRollsBackUserAndToken() throws Exception {
        doThrow(new MailSendException("SMTP unavailable")).when(sender).send(any(SimpleMailMessage.class));
        mvc.perform(post("/api/auth/register").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"fullName":"Test Reader","email":"reader@example.com","password":"%s",
                         "confirmPassword":"%s","phone":"0912345678","termsAccepted":true}
                        """.formatted(INITIAL_PASSWORD, INITIAL_PASSWORD))).andExpect(status().isServiceUnavailable());
        assertThat(users.count()).isZero();
        assertThat(tokens.count()).isZero();
    }

    @Test
    void verificationExpiresAndResendInvalidatesPriorLinkWithCooldown() throws Exception {
        localUser("reader@example.com", false);
        requestEmail("/api/auth/verification/resend", "reader@example.com");
        String original = deliveredToken();
        requestEmail("/api/auth/verification/resend", "reader@example.com");
        verify(sender, times(1)).send(any(SimpleMailMessage.class));
        AccountToken token = tokens.findAll().getFirst();
        token.setIssuedAt(Instant.now().minusSeconds(120));
        tokens.saveAndFlush(token);
        requestEmail("/api/auth/verification/resend", "reader@example.com");
        String replacement = deliveredToken();
        assertThat(replacement).isNotEqualTo(original);
        mvc.perform(get("/api/auth/verify-email").param("token", original)).andExpect(status().isBadRequest());
        AccountToken latest = tokens.findAll().stream().filter(t -> t.getUsedAt() == null).findFirst().orElseThrow();
        latest.setExpiresAt(Instant.now().minusSeconds(1));
        tokens.saveAndFlush(latest);
        mvc.perform(get("/api/auth/verify-email").param("token", replacement)).andExpect(status().isBadRequest());
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
        String known = requestEmail("/api/auth/forgot-password", "reader@example.com");
        assertThat(requestEmail("/api/auth/forgot-password", "missing@example.com")).isEqualTo(known);
        assertThat(requestEmail("/api/auth/forgot-password", "google@example.com")).isEqualTo(known);
        assertThat(requestEmail("/api/auth/forgot-password", "unverified@example.com")).isEqualTo(known);
        requestEmail("/api/auth/forgot-password", "reader@example.com");
        verify(sender, times(1)).send(any(SimpleMailMessage.class));
    }

    @Test
    void smtpFailureDuringForgotStillReturnsGenericResponse() throws Exception {
        localUser("reader@example.com", true);
        doThrow(new MailSendException("SMTP unavailable")).when(sender).send(any(SimpleMailMessage.class));
        String known = requestEmail("/api/auth/forgot-password", "reader@example.com");
        assertThat(requestEmail("/api/auth/forgot-password", "missing@example.com")).isEqualTo(known);
        assertThat(tokens.findAll()).allMatch(token -> token.getUsedAt() != null);
    }

    @Test
    void resetPasswordIsSingleUseAndRevokesLocalGoogleAndRememberMeSessions() throws Exception {
        User user = localUser("reader@example.com", true);
        MockHttpSession previous = login(user.getEmail(), INITIAL_PASSWORD);
        var remembered = mvc.perform(post("/login").with(csrf()).param("email", user.getEmail())
                .param("password", INITIAL_PASSWORD).param("remember-me", "on"))
                .andExpect(redirectedUrl("/")).andReturn().getResponse().getCookie("remember-me");
        CustomOAuth2User googlePrincipal = new CustomOAuth2User(google("google-sub", "google@example.com", true), user);
        requestEmail("/api/auth/forgot-password", user.getEmail());
        String token = deliveredToken();
        mvc.perform(get("/api/auth/reset-password").param("token", token)).andExpect(status().isOk());
        mvc.perform(post("/api/auth/reset-password").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content(resetBody(token, NEW_PASSWORD, OTHER_PASSWORD))).andExpect(status().isBadRequest());
        mvc.perform(post("/api/auth/reset-password").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content(resetBody(token, NEW_PASSWORD, NEW_PASSWORD))).andExpect(status().isOk());
        mvc.perform(post("/api/auth/reset-password").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content(resetBody(token, OTHER_PASSWORD, OTHER_PASSWORD))).andExpect(status().isBadRequest());
        mvc.perform(get("/api/account/profile").session(previous)).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/account/profile").with(oauth2Login().oauth2User(googlePrincipal)))
                .andExpect(status().isUnauthorized());
        assertThat(remembered).isNotNull();
        mvc.perform(get("/api/account/profile").cookie(remembered)).andExpect(status().isUnauthorized());
        mvc.perform(post("/login").with(csrf()).param("email", user.getEmail()).param("password", INITIAL_PASSWORD))
                .andExpect(redirectedUrl("/login?error=true"));
        login(user.getEmail(), NEW_PASSWORD);
    }

    @Test
    void resetRejectsExpiredWrongPurposeAndInvalidTokens() throws Exception {
        User user = localUser("reader@example.com", false);
        requestEmail("/api/auth/verification/resend", user.getEmail());
        String verification = deliveredToken();
        mvc.perform(post("/api/auth/reset-password").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content(resetBody(verification, NEW_PASSWORD, NEW_PASSWORD))).andExpect(status().isBadRequest());
        mvc.perform(get("/api/auth/verify-email").param("token", verification)).andExpect(status().isOk());
        requestEmail("/api/auth/forgot-password", user.getEmail());
        String reset = deliveredToken();
        mvc.perform(get("/api/auth/verify-email").param("token", reset)).andExpect(status().isBadRequest());
        AccountToken token = tokens.findAll().stream().filter(t -> t.getPurpose() == AccountTokenPurpose.RESET_PASSWORD)
                .findFirst().orElseThrow();
        token.setExpiresAt(Instant.now().minusSeconds(1));
        tokens.saveAndFlush(token);
        mvc.perform(post("/api/auth/reset-password").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content(resetBody(reset, NEW_PASSWORD, NEW_PASSWORD))).andExpect(status().isBadRequest());
        mvc.perform(get("/api/auth/reset-password").param("token", "invalid")).andExpect(status().isBadRequest());
        assertThat(passwords.matches(INITIAL_PASSWORD, users.findById(user.getId()).orElseThrow().getPassword())).isTrue();
    }

    @Test
    void concurrentResetConsumesTokenOnlyOnce() throws Exception {
        User user = localUser("reader@example.com", true);
        requestEmail("/api/auth/forgot-password", user.getEmail());
        var request = new AccountRequests.ResetPassword(deliveredToken(), NEW_PASSWORD, NEW_PASSWORD);
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
        mvc.perform(get("/api/account/profile")).andExpect(status().isUnauthorized());
        MockHttpSession session = login(user.getEmail(), INITIAL_PASSWORD);
        mvc.perform(get("/api/account/profile").session(session))
                .andExpect(status().isOk()).andExpect(jsonPath("$.email").value(user.getEmail()))
                .andExpect(jsonPath("$.password").doesNotExist()).andExpect(jsonPath("$.authVersion").doesNotExist());
        mvc.perform(put("/api/account/profile").session(session).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"fullName":"  Updated Reader  ","phone":"+84 123456789","address":"HCM",
                         "avatarUrl":"https://example.com/avatar.png","role":"ROLE_ADMIN","email":"other@example.com"}
                        """))
                .andExpect(status().isOk()).andExpect(jsonPath("$.fullName").value("Updated Reader"))
                .andExpect(jsonPath("$.role").value("ROLE_USER")).andExpect(jsonPath("$.email").value(user.getEmail()));
        assertThat(users.findById(user.getId()).orElseThrow().getFullName()).isEqualTo("Updated Reader");
        mvc.perform(put("/api/account/profile").session(session).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"fullName":"Updated Reader","avatarUrl":"javascript:alert(1)"}
                        """)).andExpect(status().isBadRequest());
    }

    @Test
    void changePasswordRequiresCurrentPasswordAndLogsOutOtherSessions() throws Exception {
        User user = localUser("reader@example.com", true);
        MockHttpSession session = login(user.getEmail(), INITIAL_PASSWORD);
        MockHttpSession other = login(user.getEmail(), INITIAL_PASSWORD);
        mvc.perform(post("/api/account/change-password").session(session).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"currentPassword":"%s","newPassword":"%s","confirmPassword":"%s"}
                        """.formatted(OTHER_PASSWORD, NEW_PASSWORD, NEW_PASSWORD))).andExpect(status().isBadRequest());
        mvc.perform(post("/api/account/change-password").session(session).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"currentPassword":"%s","newPassword":"%s","confirmPassword":"%s"}
                        """.formatted(INITIAL_PASSWORD, INITIAL_PASSWORD, INITIAL_PASSWORD))).andExpect(status().isBadRequest());
        mvc.perform(post("/api/account/change-password").session(session).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"currentPassword":"%s","newPassword":"%s","confirmPassword":"%s"}
                        """.formatted(INITIAL_PASSWORD, NEW_PASSWORD, NEW_PASSWORD))).andExpect(status().isOk());
        assertThat(session.isInvalid()).isTrue();
        mvc.perform(get("/api/account/profile").session(other)).andExpect(status().isUnauthorized());
        login(user.getEmail(), NEW_PASSWORD);
    }

    @Test
    void csrfIsRequiredAndPublicTokenEndpointSupportsApiClients() throws Exception {
        mvc.perform(post("/api/auth/forgot-password").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"reader@example.com\"}")).andExpect(status().isForbidden());
        mvc.perform(get("/api/auth/csrf")).andExpect(status().isOk())
                .andExpect(jsonPath("$.headerName").value("X-CSRF-TOKEN"))
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @ParameterizedTest
    @EnumSource(value = UserStatus.class, names = {"SUSPENDED", "EXPIRED"})
    void inactiveAccountsCannotSignInLocallyOrViaLinkedGoogle(UserStatus status) throws Exception {
        User user = localUser("reader@example.com", true);
        user.setStatus(status);
        user = users.saveAndFlush(user);
        identity(user, "blocked-google");
        mvc.perform(post("/login").with(csrf()).param("email", user.getEmail()).param("password", INITIAL_PASSWORD))
                .andExpect(redirectedUrl("/login?error=true"));
        assertThatThrownBy(() -> oauthAccounts.authenticate(google("blocked-google", "reader@example.com", true)))
                .isInstanceOf(OAuth2AuthenticationException.class);
    }

    @Test
    void disablingUserRevokesExistingSessionAndBlocksGoogle() throws Exception {
        User user = localUser("reader@example.com", true);
        MockHttpSession session = login(user.getEmail(), INITIAL_PASSWORD);
        user.setEnabled(false);
        users.saveAndFlush(user);
        identity(user, "disabled-google");
        mvc.perform(get("/api/account/profile").session(session)).andExpect(status().isUnauthorized());
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
                .password(passwords.encode(INITIAL_PASSWORD)).provider(AuthProvider.LOCAL).role(UserRole.ROLE_USER)
                .enabled(true).emailVerified(verified).status(UserStatus.ACTIVE).build());
    }

    private MockHttpSession login(String email, String password) throws Exception {
        return (MockHttpSession) mvc.perform(post("/login").with(csrf()).param("email", email).param("password", password))
                .andExpect(redirectedUrl("/")).andReturn().getRequest().getSession(false);
    }

    private String requestEmail(String path, String email) throws Exception {
        return mvc.perform(post(path).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\"}"))
                .andExpect(status().isAccepted()).andReturn().getResponse().getContentAsString();
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

    private String resetBody(String token, String password, String confirm) {
        return "{\"token\":\"" + token + "\",\"newPassword\":\"" + password + "\",\"confirmPassword\":\"" + confirm + "\"}";
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
