package grp3.java.booknet;

import grp3.java.booknet.entity.user.User;
import grp3.java.booknet.enums.user.*;
import grp3.java.booknet.repository.user.*;
import grp3.java.booknet.security.CustomOidcUser;
import grp3.java.booknet.security.CustomOidcUserService;
import grp3.java.booknet.security.OAuthLinkSessionAttributes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoderFactory;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Uses the real security filter chain; only Google's remote token exchange/decoder are mocked. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OAuthCallbackTests {
    private static final String INITIAL_PASSWORD = TestPasswords.valid();

    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired AccountTokenRepository tokens;
    @Autowired OAuthIdentityRepository identities;
    @Autowired PasswordEncoder passwords;
    @Autowired CustomOidcUserService oidcUsers;
    @MockitoBean OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> tokenClient;
    @MockitoBean JwtDecoderFactory<ClientRegistration> decoderFactory;
    private JwtDecoder decoder;

    @BeforeEach
    void prepare() {
        tokens.deleteAll();
        identities.deleteAll();
        users.deleteAll();
        decoder = mock(JwtDecoder.class);
        when(decoderFactory.createDecoder(any())).thenReturn(decoder);
        when(tokenClient.getTokenResponse(any())).thenReturn(OAuth2AccessTokenResponse.withToken("test-access-token")
                .tokenType(OAuth2AccessToken.TokenType.BEARER).expiresIn(300)
                .scopes(Set.of("openid", "profile", "email"))
                .additionalParameters(Map.of("id_token", "test-signed-id-token")).build());
        // Claims come from the mocked, validated ID token. No request reaches Google's userinfo server.
        oidcUsers.setRetrieveUserInfo(request -> false);
    }

    @Test
    void googleCallbackCreatesAccountAndPersistsUsableSession() throws Exception {
        Authorization start = startAuthorization(null);
        mockClaims(start, "new-google", "google@example.com");
        MvcResult result = callback(start).andExpect(redirectedUrl("/")).andReturn();
        MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);
        mvc.perform(get("/api/account/profile").session(session)).andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("google@example.com"))
                .andExpect(jsonPath("$.provider").value("GOOGLE"));
        SecurityContext context = (SecurityContext) session.getAttribute("SPRING_SECURITY_CONTEXT");
        assertThat(context.getAuthentication().getPrincipal()).isInstanceOf(CustomOidcUser.class);
        assertThat(users.count()).isEqualTo(1);
        assertThat(identities.count()).isEqualTo(1);
    }

    @Test
    void linkCallbackPreservesLocalAccountAndLaterGoogleLoginUsesSameUser() throws Exception {
        User local = localUser();
        MockHttpSession localSession = loginLocal();
        mvc.perform(post("/account/link/google").session(localSession).with(csrf()))
                .andExpect(redirectedUrl("/oauth2/authorization/google"));
        Authorization start = startAuthorization(localSession);
        mockClaims(start, "linked-google", "different@example.com");
        MvcResult linked = callback(start).andExpect(redirectedUrl("/?google_linked=true")).andReturn();
        MockHttpSession linkedSession = (MockHttpSession) linked.getRequest().getSession(false);
        assertLinkStateCleared(linkedSession);
        mvc.perform(get("/api/account/profile").session(linkedSession)).andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(local.getId()))
                .andExpect(jsonPath("$.provider").value("LOCAL"))
                .andExpect(jsonPath("$.email").value(local.getEmail()))
                .andExpect(jsonPath("$.googleLinked").value(true));
        assertThat(users.findById(local.getId()).orElseThrow().getPassword()).isEqualTo(local.getPassword());
        mvc.perform(post("/logout").session(linkedSession).with(csrf())).andExpect(redirectedUrl("/login?logout=true"));
        Authorization again = startAuthorization(null);
        mockClaims(again, "linked-google", "different@example.com");
        MvcResult loggedIn = callback(again).andExpect(redirectedUrl("/")).andReturn();
        mvc.perform(get("/api/account/profile").session((MockHttpSession) loggedIn.getRequest().getSession(false)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(local.getId()));
        assertThat(users.count()).isEqualTo(1);
    }

    @Test
    void sameEmailWithoutExplicitLinkDoesNotLogIntoLocalAccount() throws Exception {
        User local = localUser();
        Authorization start = startAuthorization(null);
        mockClaims(start, "unlinked-google", local.getEmail());
        MvcResult result = callback(start).andExpect(redirectedUrl("/login?oauth2_error=true")).andReturn();
        mvc.perform(get("/api/account/profile").session((MockHttpSession) result.getRequest().getSession(false)))
                .andExpect(status().isUnauthorized());
        assertThat(users.count()).isEqualTo(1);
        assertThat(identities.count()).isZero();
    }

    @Test
    void expiredLinkCallbackDoesNotCreateIdentityOrFallBackToNewAccount() throws Exception {
        localUser();
        MockHttpSession localSession = loginLocal();
        mvc.perform(post("/account/link/google").session(localSession).with(csrf()))
                .andExpect(redirectedUrl("/oauth2/authorization/google"));
        Authorization start = startAuthorization(localSession);
        start.session().setAttribute(OAuthLinkSessionAttributes.GOOGLE_LINK_STARTED_AT,
                System.currentTimeMillis() - OAuthLinkSessionAttributes.GOOGLE_LINK_MAX_AGE_MILLIS - 1000);
        mockClaims(start, "expired-google", "different@example.com");
        callback(start).andExpect(redirectedUrl("/?google_link_error=true"));
        assertLinkStateCleared(start.session());
        assertThat(identities.count()).isZero();
        assertThat(users.count()).isEqualTo(1);
    }

    @Test
    void canceledLinkClearsPendingStateAndDoesNotCreateIdentity() throws Exception {
        localUser();
        MockHttpSession localSession = loginLocal();
        mvc.perform(post("/account/link/google").session(localSession).with(csrf()))
                .andExpect(redirectedUrl("/oauth2/authorization/google"));
        Authorization start = startAuthorization(localSession);
        mvc.perform(get("/login/oauth2/code/google").session(start.session())
                .param("state", start.request().getState()).param("error", "access_denied"))
                .andExpect(redirectedUrl("/?google_link_error=true"));
        assertLinkStateCleared(start.session());
        assertThat(identities.count()).isZero();
        verifyNoInteractions(decoder);
    }

    @Test
    void wrongStateCannotAuthenticateAndDoesNotExchangeCode() throws Exception {
        Authorization start = startAuthorization(null);
        mvc.perform(get("/login/oauth2/code/google").session(start.session())
                .param("state", "wrong-state").param("code", "test-code"))
                .andExpect(redirectedUrl("/login?oauth2_error=true"));
        verify(tokenClient, never()).getTokenResponse(any());
        assertThat(users.count()).isZero();
    }

    private User localUser() {
        return users.saveAndFlush(User.builder().fullName("Local Reader").email("local@example.com")
                .password(passwords.encode(INITIAL_PASSWORD)).provider(AuthProvider.LOCAL).role(UserRole.ROLE_USER)
                .enabled(true).emailVerified(true).status(UserStatus.ACTIVE).build());
    }

    private MockHttpSession loginLocal() throws Exception {
        return (MockHttpSession) mvc.perform(post("/login").with(csrf())
                .param("email", "local@example.com").param("password", INITIAL_PASSWORD))
                .andExpect(redirectedUrl("/")).andReturn().getRequest().getSession(false);
    }

    private Authorization startAuthorization(MockHttpSession existing) throws Exception {
        var builder = get("/oauth2/authorization/google");
        if (existing != null) builder.session(existing);
        MvcResult result = mvc.perform(builder).andExpect(status().is3xxRedirection()).andReturn();
        assertThat(result.getResponse().getRedirectedUrl()).startsWith("https://accounts.google.com/");
        MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);
        OAuth2AuthorizationRequest request = Collections.list(session.getAttributeNames()).stream()
                .map(session::getAttribute).filter(OAuth2AuthorizationRequest.class::isInstance)
                .map(OAuth2AuthorizationRequest.class::cast).findFirst().orElseThrow();
        return new Authorization(session, request);
    }

    private void mockClaims(Authorization start, String subject, String email) {
        Instant now = Instant.now();
        Jwt jwt = Jwt.withTokenValue("test-signed-id-token").header("alg", "RS256")
                .issuer("https://accounts.google.com").subject(subject).audience(List.of("test-client"))
                .issuedAt(now).expiresAt(now.plusSeconds(300)).claim("email", email)
                .claim("email_verified", true).claim("name", "Google Reader")
                .claim("nonce", start.request().getAdditionalParameters().get("nonce")).build();
        when(decoder.decode("test-signed-id-token")).thenReturn(jwt);
    }

    private org.springframework.test.web.servlet.ResultActions callback(Authorization start) throws Exception {
        return mvc.perform(get("/login/oauth2/code/google").session(start.session())
                .param("state", start.request().getState()).param("code", "test-code"));
    }

    private void assertLinkStateCleared(MockHttpSession session) {
        assertThat(session.getAttribute(OAuthLinkSessionAttributes.GOOGLE_LINK_USER_ID)).isNull();
        assertThat(session.getAttribute(OAuthLinkSessionAttributes.GOOGLE_LINK_STARTED_AT)).isNull();
        assertThat(session.getAttribute(OAuthLinkSessionAttributes.GOOGLE_LINK_SUCCEEDED)).isNull();
    }

    private record Authorization(MockHttpSession session, OAuth2AuthorizationRequest request) { }
}
