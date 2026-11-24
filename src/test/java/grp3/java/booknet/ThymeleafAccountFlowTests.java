package grp3.java.booknet;

import grp3.java.booknet.entity.user.User;
import grp3.java.booknet.enums.user.*;
import grp3.java.booknet.repository.user.*;
import grp3.java.booknet.security.dto.CustomUserDetails;
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
            if (path.equals("/login")) {
                assertThat(page.getResponse().getContentAsString())
                        .contains("action=\"/login\"", "name=\"email\"", "name=\"password\"", "name=\"remember-me\"", "href=\"/oauth2/authorization/google\"")
                        .doesNotContain("auth-tabs", "Account type", "name=\"role\"");
            }
        }
        mvc.perform(get("/verification/resend")).andExpect(view().name("auth/forgot-password"))
                .andExpect(content().string(containsString("action=\"/verification/resend\"")));
        mvc.perform(get("/")).andExpect(status().isOk()).andExpect(view().name("pages/home"));
        for (String path : new String[]{"/", "/about", "/contact"}) {
            MvcResult rendered = mvc.perform(get(path)).andExpect(status().isOk()).andReturn();
            String html = rendered.getResponse().getContentAsString();
            assertThat(html).contains("id=\"booknet-navbar\"", "class=\"bn-footer\"",
                    "id=\"bn-menu-toggle\"", "id=\"bn-nav-links\"",
                    "href=\"/css/base/tokens.css\"", "href=\"/css/layout/navbar.css\"", "href=\"/css/layout/footer.css\"");
            assertThat(html).doesNotContain("class=\"admin-sidebar\"");
            assertThat(html.indexOf("/css/base/tokens.css")).isLessThan(html.indexOf("/css/layout/navbar.css"));
        }
        for (String path : new String[]{"/login", "/register", "/forgot-password"}) {
            mvc.perform(get(path)).andExpect(content().string(containsString("class=\"auth-aside\"")))
                    .andExpect(content().string(containsString("class=\"auth-aside-photo\"")))
                    .andExpect(content().string(containsString("href=\"/css/pages/auth.css\"")));
        }
        mvc.perform(get("/about").header("Accept-Language", "vi-VN")).andExpect(status().isOk()).andExpect(view().name("pages/about"))
                .andExpect(content().string(containsString("lang=\"en\"")))
                .andExpect(content().string(containsString("About booknet")))
                .andExpect(content().string(containsString("Explore the catalog")))
                .andExpect(content().string(containsString("Selected with care")))
                .andExpect(content().string(containsString("Become a member for free")));
        FormPage contact = formPage("/contact");
        assertThat(contact.html()).contains("lang=\"en\"", "Contact & Support", "Send us a message",
                "Full name", "Email address", "Borrowing & renewals", "How are overdue fines calculated?",
                "Get directions on Google Maps", "action=\"/contact\"", "/js/auth.js").doesNotContain("th:action");
        mvc.perform(get("/reset-password")).andExpect(status().isOk()).andExpect(view().name("auth/reset-password"))
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
                .andExpect(status().isOk()).andExpect(content().string(containsString("verify your account before signing in")));
        assertThat(deliveredMessage().getText()).contains("/verify-email?token=").doesNotContain("/api/auth/");
        assertThat(deliveredMessage().getSubject()).isEqualTo("BookNet - Verify your email");
        assertThat(deliveredMessage().getText()).contains("valid for 24 hours, one use only");
    }

    @Test
    void invalidRegistrationRendersBoundErrorsWithoutCreatingAccount() throws Exception {
        mvc.perform(post("/register").with(csrf()).header("Accept-Language", "vi-VN").param("fullName", "Form Reader")
                .param("email", "reader@example.com").param("phone", "not-a-phone")
                .param("password", "weakpass").param("confirmPassword", "different").param("termsAccepted", "true"))
                .andExpect(status().isOk()).andExpect(view().name("auth/register"))
                .andExpect(model().attributeHasFieldErrors("user", "phone", "password", "confirmPassword"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(containsString("Please enter a valid phone number.")))
                .andExpect(content().string(containsString("Passwords do not match.")));
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
                .andExpect(status().isOk()).andExpect(view().name("auth/reset-password"))
                .andExpect(model().attributeExists("confirmPasswordError"))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("value=\"" + NEW_PASSWORD + "\""))));
        mvc.perform(post("/reset-password").session(reset.session()).param("_csrf", reset.csrf())
                .param("token", token).param("password", NEW_PASSWORD).param("confirmPassword", NEW_PASSWORD))
                .andExpect(redirectedUrl("/login?reset=true"));
        mvc.perform(get("/login").param("reset", "true")).andExpect(status().isOk())
                .andExpect(content().string(containsString("sign in with your new password")));
        mvc.perform(get("/reset-password").param("token", token)).andExpect(status().isOk())
                .andExpect(model().attribute("tokenValid", false));
        mvc.perform(post("/login").with(csrf()).param("email", "reader@example.com").param("password", INITIAL_PASSWORD))
                .andExpect(redirectedUrl("/login?error=true"));
        mvc.perform(post("/login").with(csrf()).param("email", "reader@example.com").param("password", NEW_PASSWORD))
                .andExpect(redirectedUrl("/account/profile"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"Short1A", "lowercase123", "UPPERCASE123", "MissingDigits"})
    void serverRejectsPasswordsThatBypassHtmlPolicy(String password) throws Exception {
        localUser(true);
        String token = requestResetThroughForm();
        mvc.perform(post("/reset-password").with(csrf()).param("token", token)
                .param("password", password).param("confirmPassword", password))
                .andExpect(status().isOk()).andExpect(view().name("auth/reset-password"))
                .andExpect(model().attribute("tokenValid", true)).andExpect(model().attributeExists("passwordError"));
        assertThat(tokens.findAll().getFirst().getUsedAt()).isNull();
        assertThat(passwords.matches(INITIAL_PASSWORD, users.findByEmail("reader@example.com").orElseThrow().getPassword())).isTrue();
    }

    @Test
    void malformedFormRequestsRenderErrorsAndRequireCsrf() throws Exception {
        mvc.perform(post("/forgot-password").with(csrf()).param("email", "invalid"))
                .andExpect(status().isOk()).andExpect(view().name("auth/forgot-password"))
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
                .andExpect(content().string(containsString("Email verified successfully")));
        MvcResult reused = mvc.perform(get("/verify-email").param("token", token))
                .andExpect(redirectedUrl("/login")).andExpect(flash().attributeExists("errorMessage")).andReturn();
        mvc.perform(get("/login").flashAttrs(reused.getFlashMap())).andExpect(status().isOk())
                .andExpect(content().string(containsString("This verification link is invalid")))
                .andExpect(content().string(containsString("href=\"/verification/resend\"")));
        mvc.perform(post("/login").with(csrf()).param("email", "reader@example.com").param("password", INITIAL_PASSWORD))
                .andExpect(redirectedUrl("/account/profile"));
    }

    @Test
    void profileAndPasswordFormsSubmitRenderedCsrfAndShowFlashMessages() throws Exception {
        localUser(true);
        MockHttpSession session = login();
        FormPage profile = formPage("/account/profile", session);
        assertThat(profile.html()).contains("action=\"/account/profile\"", "name=\"fullName\"", "name=\"phone\"",
                "name=\"address\"", "name=\"avatarUrl\"").doesNotContain("th:field", "name=\"password\"");
        MvcResult updated = mvc.perform(post("/account/profile").session(session).param("_csrf", profile.csrf())
                .param("fullName", "Updated Reader").param("phone", "0912345678").param("address", "HCM"))
                .andExpect(redirectedUrl("/account/profile")).andReturn();
        mvc.perform(get("/account/profile").session(session).flashAttrs(updated.getFlashMap()))
                .andExpect(content().string(containsString("Your profile has been updated")))
                .andExpect(content().string(containsString("value=\"Updated Reader\"")));
        FormPage password = formPage("/account/change-password", session);
        assertThat(password.html()).contains("action=\"/account/change-password\"", "name=\"currentPassword\"",
                "name=\"newPassword\"", "name=\"confirmPassword\"");
        MvcResult changed = mvc.perform(post("/account/change-password").session(session).param("_csrf", password.csrf())
                .param("currentPassword", INITIAL_PASSWORD).param("newPassword", NEW_PASSWORD).param("confirmPassword", NEW_PASSWORD))
                .andExpect(redirectedUrl("/login")).andReturn();
        assertThat(session.isInvalid()).isTrue();
        assertThat(changed.getResponse().getCookie("remember-me").getMaxAge()).isZero();
        mvc.perform(get("/login").flashAttrs(changed.getFlashMap()))
                .andExpect(content().string(containsString("Your password has been changed")));
    }

    @Test
    void accountFormsRenderErrorsWithoutEchoingPasswordsAndRejectMissingCsrf() throws Exception {
        localUser(true);
        MockHttpSession session = login();
        mvc.perform(post("/account/profile").session(session).param("fullName", "Changed"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/account/change-password").session(session).param("currentPassword", INITIAL_PASSWORD))
                .andExpect(status().isForbidden());
        mvc.perform(post("/account/profile").session(session).with(csrf()).param("fullName", "X"))
                .andExpect(view().name("account/profile")).andExpect(model().attributeHasFieldErrors("profileForm", "fullName"))
                .andExpect(content().string(containsString("role=\"alert\"")))
                .andExpect(content().string(containsString("Full name must be between 2 and 120 characters.")));
        mvc.perform(post("/account/change-password").session(session).with(csrf()).param("currentPassword", INITIAL_PASSWORD)
                .param("newPassword", NEW_PASSWORD).param("confirmPassword", OTHER_PASSWORD))
                .andExpect(view().name("account/change-password")).andExpect(model().attributeHasErrors("passwordForm"))
                .andExpect(content().string(containsString("Passwords do not match")))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("value=\"" + INITIAL_PASSWORD + "\""))))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("value=\"" + NEW_PASSWORD + "\""))));
        assertThat(passwords.matches(INITIAL_PASSWORD, users.findByEmail("reader@example.com").orElseThrow().getPassword())).isTrue();
    }

    @Test
    void googleAccountSetsPasswordThroughEmailAndKeepsGoogleIdentity() throws Exception {
        localUser(true);
        User user = users.findByEmail("reader@example.com").orElseThrow();
        user.setProvider(AuthProvider.GOOGLE);
        user.setPassword(null);
        users.saveAndFlush(user);
        identities.saveAndFlush(grp3.java.booknet.entity.user.OAuthIdentity.builder()
                .user(user).provider(AuthProvider.GOOGLE).providerUserId("google-only")
                .providerSubject("google-only").email(user.getEmail()).build());
        var google = new grp3.java.booknet.security.principal.CustomOAuth2User(
                new org.springframework.security.oauth2.core.user.DefaultOAuth2User(
                        java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_MEMBER")),
                        java.util.Map.of("sub", "google-only", "email", user.getEmail()), "sub"), user);
        var authentication = org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login().oauth2User(google);
        MvcResult profile = mvc.perform(get("/account/profile").with(authentication)).andExpect(view().name("account/profile"))
                .andExpect(content().string(containsString("action=\"/account/set-password\"")))
                .andExpect(content().string(containsString("Set password"))).andReturn();
        mvc.perform(post("/account/profile").with(authentication).with(csrf()).param("fullName", "Google Reader"))
                .andExpect(redirectedUrl("/account/profile"));
        mvc.perform(get("/account/change-password").with(authentication))
                .andExpect(content().string(containsString("This account uses Google sign-in")))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("name=\"currentPassword\""))));
        mvc.perform(post("/account/change-password").with(authentication).with(csrf()).param("currentPassword", INITIAL_PASSWORD)
                .param("newPassword", NEW_PASSWORD).param("confirmPassword", NEW_PASSWORD))
                .andExpect(view().name("account/change-password"));
        assertThat(users.findById(user.getId()).orElseThrow().getPassword()).isNull();

        mvc.perform(post("/login").with(csrf()).param("email", user.getEmail()).param("password", INITIAL_PASSWORD))
                .andExpect(redirectedUrl("/login?error=true"));
        mvc.perform(post("/account/set-password").with(authentication)).andExpect(status().isForbidden());
        String html = profile.getResponse().getContentAsString();
        var csrfToken = Pattern.compile("name=\"_csrf\"[^>]*value=\"([^\"]+)\"").matcher(html);
        assertThat(csrfToken.find()).isTrue();
        mvc.perform(post("/account/set-password").with(authentication)
                        .session((MockHttpSession) profile.getRequest().getSession(false)).param("_csrf", csrfToken.group(1))
                        .param("email", "attacker@example.com"))
                .andExpect(redirectedUrl("/account/profile")).andExpect(flash().attributeExists("successMessage"));
        assertThat(deliveredMessage().getTo()).containsExactly(user.getEmail());
        String token = deliveredToken();
        assertThat(tokens.findAll().getFirst().getTokenHash()).isNotEqualTo(token);
        assertThat(Duration.between(tokens.findAll().getFirst().getIssuedAt(), tokens.findAll().getFirst().getExpiresAt()))
                .isEqualTo(Duration.ofMinutes(15));
        mvc.perform(post("/account/set-password").with(authentication).with(csrf()))
                .andExpect(redirectedUrl("/account/profile"));
        verify(sender, times(1)).send(any(SimpleMailMessage.class));

        FormPage reset = formPage("/reset-password?token=" + token);
        mvc.perform(post("/reset-password").session(reset.session()).param("_csrf", reset.csrf())
                        .param("token", token).param("password", NEW_PASSWORD).param("confirmPassword", OTHER_PASSWORD))
                .andExpect(model().attributeExists("confirmPasswordError"));
        mvc.perform(post("/reset-password").session(reset.session()).param("_csrf", reset.csrf())
                        .param("token", token).param("password", NEW_PASSWORD).param("confirmPassword", NEW_PASSWORD))
                .andExpect(redirectedUrl("/login?reset=true"));
        User updated = users.findById(user.getId()).orElseThrow();
        assertThat(updated.getProvider()).isEqualTo(AuthProvider.GOOGLE);
        assertThat(passwords.matches(NEW_PASSWORD, updated.getPassword())).isTrue();
        assertThat(updated.getAuthVersion()).isEqualTo(1);
        assertThat(identities.findByProviderAndProviderUserId(AuthProvider.GOOGLE, "google-only").orElseThrow().getUser().getId())
                .isEqualTo(user.getId());
        mvc.perform(get("/reset-password").param("token", token)).andExpect(model().attribute("tokenValid", false));
        mvc.perform(get("/account/profile").with(authentication)).andExpect(redirectedUrl("/login?error=true"));
        MockHttpSession localSession = (MockHttpSession) mvc.perform(post("/login").with(csrf())
                        .param("email", user.getEmail()).param("password", NEW_PASSWORD))
                .andExpect(redirectedUrl("/account/profile")).andReturn().getRequest().getSession(false);
        mvc.perform(get("/account/profile").session(localSession))
                .andExpect(content().string(containsString("Change password")))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("action=\"/account/set-password\""))));
        mvc.perform(post("/account/set-password").session(localSession).with(csrf()))
                .andExpect(flash().attributeExists("errorMessage"));
        mvc.perform(post("/account/change-password").session(localSession).with(csrf())
                        .param("currentPassword", NEW_PASSWORD).param("newPassword", OTHER_PASSWORD).param("confirmPassword", OTHER_PASSWORD))
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void passwordSetupSmtpFailurePreservesAccountAndAllowsRetry() throws Exception {
        localUser(true);
        User user = users.findByEmail("reader@example.com").orElseThrow();
        user.setProvider(AuthProvider.GOOGLE);
        user.setPassword(null);
        users.saveAndFlush(user);
        var principal = new CustomUserDetails(user);
        var authentication = org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user(principal);
        doThrow(new org.springframework.mail.MailSendException("SMTP unavailable")).when(sender).send(any(SimpleMailMessage.class));
        MvcResult failed = mvc.perform(post("/account/set-password").with(authentication).with(csrf()))
                .andExpect(redirectedUrl("/account/profile")).andExpect(flash().attributeExists("errorMessage"))
                .andExpect(flash().attributeCount(1)).andReturn();
        mvc.perform(get("/account/profile").with(authentication).flashAttrs(failed.getFlashMap()))
                .andExpect(content().string(containsString("Unable to send the password setup email")));
        assertThat(tokens.count()).isZero();
        assertThat(users.findById(user.getId()).orElseThrow().getPassword()).isNull();
        reset(sender);
        mvc.perform(post("/account/set-password").with(authentication).with(csrf()))
                .andExpect(flash().attributeExists("successMessage"));
        verify(sender).send(any(SimpleMailMessage.class));
    }

    @Test
    void contactSubmitsRenderedFormAndSendsToLibrariansWithReplyTo() throws Exception {
        FormPage form = formPage("/contact");
        MvcResult sent = mvc.perform(post("/contact").session(form.session()).param("_csrf", form.csrf())
                        .param("fullName", "  Contact Reader  ").param("email", "reader@example.com")
                        .param("subject", "Book donations").param("message", "I would like to donate books."))
                .andExpect(redirectedUrl("/contact")).andExpect(flash().attributeExists("contactSuccess")).andReturn();
        SimpleMailMessage mail = deliveredMessage();
        assertThat(mail.getTo()).containsExactly("librarians@booknet.test");
        assertThat(mail.getFrom()).isEqualTo("no-reply@booknet.test");
        assertThat(mail.getReplyTo()).isEqualTo("reader@example.com");
        assertThat(mail.getSubject()).isEqualTo("BookNet - Contact: Book donations");
        assertThat(mail.getText()).contains("Contact Reader", "reader@example.com", "I would like to donate books.");
        mvc.perform(get("/contact").flashAttrs(sent.getFlashMap()))
                .andExpect(content().string(containsString("Your message has been submitted")));
        verify(sender, times(1)).send(any(SimpleMailMessage.class));
    }

    @Test
    void contactRejectsMissingCsrfAndInvalidFieldsWithoutSending() throws Exception {
        mvc.perform(post("/contact").param("fullName", "Reader")).andExpect(status().isForbidden());
        mvc.perform(post("/contact").with(csrf()).param("fullName", " ").param("email", "invalid")
                        .param("subject", "Other\r\nBcc: attacker@example.com").param("message", " "))
                .andExpect(view().name("pages/contact")).andExpect(model().attribute("activePage", "contact"))
                .andExpect(model().attributeHasFieldErrors("contactForm", "fullName", "email", "subject", "message"))
                .andExpect(content().string(containsString("Please enter your message.")));
        mvc.perform(post("/contact").with(csrf()).param("fullName", "X".repeat(121))
                        .param("email", "x".repeat(110) + "@example.com").param("message", "X".repeat(5001)))
                .andExpect(model().attributeHasFieldErrors("contactForm", "fullName", "email", "message"));
        verifyNoInteractions(sender);
    }

    @Test
    void contactSmtpFailureRendersErrorAndPreservesEscapedInput() throws Exception {
        doThrow(new org.springframework.mail.MailSendException("SMTP unavailable")).when(sender).send(any(SimpleMailMessage.class));
        mvc.perform(post("/contact").with(csrf()).param("fullName", "Reader").param("email", "reader@example.com")
                        .param("subject", "Other").param("message", "<script>alert('test')</script>"))
                .andExpect(view().name("pages/contact")).andExpect(model().attributeHasErrors("contactForm"))
                .andExpect(model().attributeDoesNotExist("contactSuccess"))
                .andExpect(content().string(containsString("Unable to send your message")))
                .andExpect(content().string(containsString("value=\"reader@example.com\"")))
                .andExpect(content().string(containsString("&lt;script&gt;")))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("<script>alert"))));
    }

    @ParameterizedTest
    @ValueSource(strings = {"Short1A", "lowercase123", "UPPERCASE123", "MissingDigits", "Ab1xxxxxxxxxxxxxxxxxxxxxxxxxxxxxx", "Ab1漢漢漢漢漢漢漢漢漢漢漢漢漢漢漢漢漢漢漢漢漢漢漢漢"})
    void registrationRejectsInvalidPasswordsWithoutCreatingUser(String password) throws Exception {
        mvc.perform(post("/register").with(csrf()).param("fullName", "Form Reader")
                .param("email", "reader@example.com").param("phone", "0912345678").param("termsAccepted", "true")
                .param("password", password).param("confirmPassword", password))
                .andExpect(view().name("auth/register")).andExpect(content().string(containsString("72 UTF-8 bytes")))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("value=\"" + password + "\""))));
        assertThat(users.count()).isZero();
        verifyNoInteractions(sender);
    }

    @Test
    void registrationRequiresCsrfAndLengthLimitsMatchServerValidation() throws Exception {
        mvc.perform(post("/register").param("email", "reader@example.com")).andExpect(status().isForbidden());
        mvc.perform(post("/register").with(csrf()).param("fullName", "X".repeat(121))
                .param("email", "x".repeat(110) + "@example.com").param("phone", "0912345678")
                .param("password", INITIAL_PASSWORD).param("confirmPassword", INITIAL_PASSWORD).param("termsAccepted", "true"))
                .andExpect(view().name("auth/register")).andExpect(model().attributeHasFieldErrors("user", "fullName", "email"));
        assertThat(users.count()).isZero();
        verifyNoInteractions(sender);
    }

    private MockHttpSession login() throws Exception {
        return (MockHttpSession) mvc.perform(post("/login").with(csrf()).param("email", "reader@example.com")
                .param("password", INITIAL_PASSWORD)).andExpect(redirectedUrl("/account/profile")).andReturn().getRequest().getSession(false);
    }

    private String requestResetThroughForm() throws Exception {
        FormPage form = formPage("/forgot-password");
        MvcResult sent = mvc.perform(post("/forgot-password").session(form.session()).param("_csrf", form.csrf())
                .param("email", "reader@example.com")).andExpect(redirectedUrl("/forgot-password")).andReturn();
        mvc.perform(get("/forgot-password").flashAttrs(sent.getFlashMap())).andExpect(status().isOk())
                .andExpect(content().string(containsString("Request submitted")));
        assertThat(deliveredMessage().getText()).contains("/reset-password?token=").doesNotContain("/api/auth/");
        assertThat(deliveredMessage().getSubject()).isEqualTo("BookNet - Reset your password");
        assertThat(deliveredMessage().getText()).contains("valid for 15 minutes, one use only", "If you did not request this");
        return deliveredToken();
    }

    private FormPage formPage(String path) throws Exception {
        return formPage(path, null);
    }

    private FormPage formPage(String path, MockHttpSession session) throws Exception {
        var request = get(path);
        if (session != null) request.session(session);
        MvcResult result = mvc.perform(request).andExpect(status().isOk())
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
