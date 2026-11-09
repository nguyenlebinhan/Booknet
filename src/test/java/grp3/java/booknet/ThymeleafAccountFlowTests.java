package grp3.java.booknet;

import grp3.java.booknet.entity.user.User;
import grp3.java.booknet.enums.user.*;
import grp3.java.booknet.repository.user.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.*;

import java.time.Duration;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Render Thymeleaf and submit its real form names and CSRF token, rather than JSON only. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ThymeleafAccountFlowTests {
    private static final String INITIAL_PASSWORD = TestPasswords.valid();
    private static final String NEW_PASSWORD = TestPasswords.valid();
    private static final String OTHER_PASSWORD = TestPasswords.valid();

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
        reset(sender);
    }

    @Test
    void publicPagesRenderHtmlWithCorrectFormsAndSecurityFields() throws Exception {
        for (String path : new String[]{"/login", "/register", "/forgot-password", "/verification/resend"}) {
            MvcResult page = mvc.perform(get(path)).andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML)).andReturn();
            assertThat(page.getResponse().getContentAsString()).contains("name=\"_csrf\"").doesNotContain("th:action");
        }
        mvc.perform(get("/verification/resend")).andExpect(view().name("forgot-password"))
                .andExpect(content().string(containsString("action=\"/verification/resend\"")));
        mvc.perform(get("/")).andExpect(status().isOk()).andExpect(view().name("home"));
        mvc.perform(get("/reset-password")).andExpect(status().isOk()).andExpect(view().name("reset-password"))
                .andExpect(model().attribute("tokenValid", false))
                .andExpect(content().string(containsString("This link has expired.")));
    }

    @Test
    void registrationFormSavesPhoneAndRendersVerificationInstruction() throws Exception {
        FormPage form = formPage("/register");
        MvcResult registered = mvc.perform(post("/register").session(form.session()).param("_csrf", form.csrf())
                .param("fullName", "Form Reader").param("email", "Form@Example.com").param("phone", "0912345678")
                .param("password", INITIAL_PASSWORD).param("confirmPassword", INITIAL_PASSWORD).param("termsAccepted", "true"))
                .andExpect(redirectedUrl("/login?registered=true")).andReturn();
        User saved = users.findByEmail("form@example.com").orElseThrow();
        assertThat(saved.getPhone()).isEqualTo("0912345678");
        assertThat(saved.isEmailVerified()).isFalse();
        mvc.perform(get("/login").param("registered", "true").flashAttrs(registered.getFlashMap()))
                .andExpect(status().isOk()).andExpect(content().string(containsString("xác thực tài khoản trước khi đăng nhập")));
        assertThat(deliveredMessage().getText()).contains("/verify-email?token=").doesNotContain("/api/auth/");
    }

    @Test
    void invalidRegistrationRendersBoundErrorsWithoutCreatingAccount() throws Exception {
        mvc.perform(post("/register").with(csrf()).param("fullName", "Form Reader")
                .param("email", "reader@example.com").param("phone", "not-a-phone")
                .param("password", "weakpass").param("confirmPassword", "different").param("termsAccepted", "true"))
                .andExpect(status().isOk()).andExpect(view().name("register"))
                .andExpect(model().attributeHasFieldErrors("user", "phone", "password", "confirmPassword"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML));
        assertThat(users.count()).isZero();
        verifyNoInteractions(sender);
    }

    @Test
    void passwordRecoveryRunsThroughRenderedFormsAndRedirectsToLogin() throws Exception {
        localUser(true);
        String token = requestResetThroughForm();
        var saved = tokens.findAll().getFirst();
        assertThat(Duration.between(saved.getIssuedAt(), saved.getExpiresAt())).isEqualTo(Duration.ofMinutes(15));
        FormPage reset = formPage("/reset-password?token=" + token);
        assertThat(reset.html()).contains("action=\"/reset-password\"", "name=\"token\" value=\"" + token + "\"");
        mvc.perform(post("/reset-password").session(reset.session()).param("_csrf", reset.csrf())
                .param("token", token).param("password", NEW_PASSWORD).param("confirmPassword", OTHER_PASSWORD))
                .andExpect(status().isOk()).andExpect(view().name("reset-password"))
                .andExpect(model().attributeExists("confirmPasswordError"))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("value=\"" + NEW_PASSWORD + "\""))));
        mvc.perform(post("/reset-password").session(reset.session()).param("_csrf", reset.csrf())
                .param("token", token).param("password", NEW_PASSWORD).param("confirmPassword", NEW_PASSWORD))
                .andExpect(redirectedUrl("/login?reset=true"));
        mvc.perform(get("/login").param("reset", "true")).andExpect(status().isOk())
                .andExpect(content().string(containsString("đăng nhập bằng mật khẩu mới")));
        mvc.perform(get("/reset-password").param("token", token)).andExpect(status().isOk())
                .andExpect(model().attribute("tokenValid", false));
        mvc.perform(post("/login").with(csrf()).param("email", "reader@example.com").param("password", INITIAL_PASSWORD))
                .andExpect(redirectedUrl("/login?error=true"));
        mvc.perform(post("/login").with(csrf()).param("email", "reader@example.com").param("password", NEW_PASSWORD))
                .andExpect(redirectedUrl("/"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"Short1A", "lowercase123", "UPPERCASE123", "MissingDigits"})
    void serverRejectsPasswordsThatBypassHtmlPolicy(String password) throws Exception {
        localUser(true);
        String token = requestResetThroughForm();
        mvc.perform(post("/reset-password").with(csrf()).param("token", token)
                .param("password", password).param("confirmPassword", password))
                .andExpect(status().isOk()).andExpect(view().name("reset-password"))
                .andExpect(model().attribute("tokenValid", true)).andExpect(model().attributeExists("passwordError"));
        assertThat(tokens.findAll().getFirst().getUsedAt()).isNull();
        assertThat(passwords.matches(INITIAL_PASSWORD, users.findByEmail("reader@example.com").orElseThrow().getPassword())).isTrue();
    }

    @Test
    void malformedFormRequestsRenderErrorsAndRequireCsrf() throws Exception {
        mvc.perform(post("/forgot-password").with(csrf()).param("email", "invalid"))
                .andExpect(status().isOk()).andExpect(view().name("forgot-password"))
                .andExpect(model().attributeExists("emailError"));
        mvc.perform(post("/forgot-password").param("email", "reader@example.com")).andExpect(status().isForbidden());
        mvc.perform(post("/reset-password").param("token", "invalid")).andExpect(status().isForbidden());
        mvc.perform(get("/reset-password").param("token", "invalid")).andExpect(status().isOk())
                .andExpect(model().attribute("tokenValid", false));
        verifyNoInteractions(sender);
    }

    @Test
    void verificationResendAndEmailLinkUseHtmlAndFlashMessages() throws Exception {
        localUser(false);
        MvcResult sent = mvc.perform(post("/verification/resend").with(csrf()).param("email", "reader@example.com"))
                .andExpect(redirectedUrl("/verification/resend")).andReturn();
        mvc.perform(get("/verification/resend").flashAttrs(sent.getFlashMap())).andExpect(status().isOk())
                .andExpect(content().string(containsString("Request submitted")));
        String token = deliveredToken();
        MvcResult verified = mvc.perform(get("/verify-email").param("token", token))
                .andExpect(redirectedUrl("/login")).andExpect(flash().attributeExists("successMessage")).andReturn();
        mvc.perform(get("/login").flashAttrs(verified.getFlashMap())).andExpect(status().isOk())
                .andExpect(content().string(containsString("Xác thực email thành công")));
        MvcResult reused = mvc.perform(get("/verify-email").param("token", token))
                .andExpect(redirectedUrl("/login")).andExpect(flash().attributeExists("errorMessage")).andReturn();
        mvc.perform(get("/login").flashAttrs(reused.getFlashMap())).andExpect(status().isOk())
                .andExpect(content().string(containsString("Liên kết xác thực không hợp lệ")))
                .andExpect(content().string(containsString("href=\"/verification/resend\"")));
        mvc.perform(post("/login").with(csrf()).param("email", "reader@example.com").param("password", INITIAL_PASSWORD))
                .andExpect(redirectedUrl("/"));
    }

    private String requestResetThroughForm() throws Exception {
        FormPage form = formPage("/forgot-password");
        MvcResult sent = mvc.perform(post("/forgot-password").session(form.session()).param("_csrf", form.csrf())
                .param("email", "reader@example.com")).andExpect(redirectedUrl("/forgot-password")).andReturn();
        mvc.perform(get("/forgot-password").flashAttrs(sent.getFlashMap())).andExpect(status().isOk())
                .andExpect(content().string(containsString("Request submitted")));
        assertThat(deliveredMessage().getText()).contains("/reset-password?token=").doesNotContain("/api/auth/");
        return deliveredToken();
    }

    private FormPage formPage(String path) throws Exception {
        MvcResult result = mvc.perform(get(path)).andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML)).andReturn();
        String html = result.getResponse().getContentAsString();
        var matcher = Pattern.compile("name=\"_csrf\"[^>]*value=\"([^\"]+)\"").matcher(html);
        assertThat(matcher.find()).isTrue();
        return new FormPage((MockHttpSession) result.getRequest().getSession(false), matcher.group(1), html);
    }

    private SimpleMailMessage deliveredMessage() {
        return mockingDetails(sender).getInvocations().stream()
                .filter(invocation -> invocation.getArgument(0) instanceof SimpleMailMessage)
                .map(invocation -> (SimpleMailMessage) invocation.getArgument(0)).toList().getLast();
    }

    private String deliveredToken() {
        var matcher = Pattern.compile("[?&]token=([A-Za-z0-9_-]+)").matcher(deliveredMessage().getText());
        assertThat(matcher.find()).isTrue();
        return matcher.group(1);
    }

    private void localUser(boolean verified) {
        users.saveAndFlush(User.builder().fullName("Test Reader").email("reader@example.com")
                .password(passwords.encode(INITIAL_PASSWORD)).provider(AuthProvider.LOCAL).role(UserRole.ROLE_USER)
                .enabled(true).emailVerified(verified).status(UserStatus.ACTIVE).build());
    }

    private record FormPage(MockHttpSession session, String csrf, String html) { }
}
