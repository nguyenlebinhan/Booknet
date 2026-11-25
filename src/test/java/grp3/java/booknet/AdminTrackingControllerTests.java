package grp3.java.booknet;

import grp3.java.booknet.config.tracking.LoginSuccessListener;
import grp3.java.booknet.controller.admin.AdminTrackingController;
import grp3.java.booknet.entity.tracking.PageVisitLog;
import grp3.java.booknet.entity.tracking.UserLoginLog;
import grp3.java.booknet.entity.user.User;
import grp3.java.booknet.enums.user.AuthProvider;
import grp3.java.booknet.enums.user.UserRole;
import grp3.java.booknet.enums.user.UserStatus;
import grp3.java.booknet.repository.tracking.PageVisitLogRepository;
import grp3.java.booknet.repository.tracking.UserLoginLogRepository;
import grp3.java.booknet.repository.user.UserRepository;
import grp3.java.booknet.security.dto.CustomUserDetails;
import grp3.java.booknet.security.principal.CustomOAuth2User;
import grp3.java.booknet.service.tracking.IpLocationResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.event.InteractiveAuthenticationSuccessEvent;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminTrackingControllerTests {

    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired UserLoginLogRepository loginLogRepo;
    @Autowired PageVisitLogRepository pageVisitRepo;
    @Autowired IpLocationResolver ipLocationResolver;
    @Autowired LoginSuccessListener loginSuccessListener;
    @MockitoBean JavaMailSender mailSender;

    private static final String RAW_PASSWORD = TestPasswords.valid();

    private User admin;
    private User librarian;
    private User member;

    @BeforeEach
    void setUp() {
        pageVisitRepo.deleteAll();
        loginLogRepo.deleteAll();

        admin = ensureUser("Tracking Admin", "tracking-admin@booknet.test", UserRole.ROLE_ADMIN);
        librarian = ensureUser("Tracking Librarian", "tracking-librarian@booknet.test", UserRole.ROLE_LIBRARIAN);
        member = ensureUser("Tracking Member", "tracking-member@booknet.test", UserRole.ROLE_USER);
    }

    private User ensureUser(String name, String email, UserRole role) {
        return users.findByEmail(email).orElseGet(() -> users.saveAndFlush(
                User.builder()
                        .fullName(name)
                        .email(email)
                        .password(passwordEncoder.encode(RAW_PASSWORD))
                        .role(role)
                        .provider(AuthProvider.LOCAL)
                        .status(UserStatus.ACTIVE)
                        .enabled(true)
                        .emailVerified(true)
                        .build()
        ));
    }

    @Test
    void trackingEndpointEnforcesAdminAuthorization() throws Exception {
        // Unauthenticated -> redirect to login
        mvc.perform(get("/admin/tracking"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        // Member -> 403 Forbidden
        mvc.perform(get("/admin/tracking").with(user(new CustomUserDetails(member))))
                .andExpect(status().isForbidden());

        // Librarian -> 403 Forbidden
        mvc.perform(get("/admin/tracking").with(user(new CustomUserDetails(librarian))))
                .andExpect(status().isForbidden());

        // Admin -> 200 OK
        String html = mvc.perform(get("/admin/tracking").with(user(new CustomUserDetails(admin))))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/tracking/index"))
                .andExpect(model().attribute("activePage", "tracking"))
                .andExpect(model().attributeExists("totalLogins", "loginsToday", "uniqueActiveUsers",
                        "totalPageVisits", "topPages", "loginLocations", "visitLocations", "loginLogs", "visitLogs"))
                .andReturn().getResponse().getContentAsString();

        assertThat(html)
                .contains("User Tracking Dashboard", "href=\"/admin/tracking\"", "User tracking")
                .doesNotContain("th:text=", "th:each=");
    }

    @Test
    void sidebarShowsUserTrackingOnlyToAdmin() throws Exception {
        String staffHtml = mvc.perform(get("/staff/dashboard").with(user(new CustomUserDetails(librarian))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(staffHtml).doesNotContain("href=\"/admin/tracking\"");
    }

    @Test
    void rendersPopulatedLogsTopPagesLocationsAndFiltersCleanly() throws Exception {
        LocalDateTime now = LocalDateTime.now();

        loginLogRepo.saveAll(List.of(
                UserLoginLog.builder()
                        .userEmail("alice@booknet.test")
                        .userName("Alice Reader")
                        .userRole("MEMBER")
                        .loginAt(now.minusHours(2))
                        .ipAddress("127.0.0.1")
                        .location("Hanoi, Vietnam (Local)")
                        .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/130.0.0.0 Safari/537.36 VeryLongUserAgentSuffixForTruncationTest")
                        .loginMethod("PASSWORD")
                        .build(),
                UserLoginLog.builder()
                        .userEmail("oauth-bob@booknet.test")
                        .userName("Bob Google")
                        .userRole("LIBRARIAN")
                        .loginAt(now.minusDays(2))
                        .ipAddress("203.0.113.5")
                        .location("Singapore, Singapore")
                        .userAgent("Mozilla/5.0 (X11; Linux x86_64; rv:131.0) Gecko/20100101 Firefox/131.0")
                        .loginMethod("GOOGLE_OAUTH2")
                        .build()
        ));

        pageVisitRepo.saveAll(List.of(
                PageVisitLog.builder()
                        .userEmail("alice@booknet.test")
                        .userRole("MEMBER")
                        .pagePath("/catalog")
                        .pageName("Book Catalog")
                        .httpMethod("GET")
                        .ipAddress("127.0.0.1")
                        .location("Hanoi, Vietnam (Local)")
                        .visitedAt(now.minusMinutes(30))
                        .build(),
                PageVisitLog.builder()
                        .userEmail("Anonymous (Guest)")
                        .userRole("GUEST")
                        .pagePath("/catalog")
                        .pageName("Book Catalog")
                        .httpMethod("GET")
                        .ipAddress("127.0.0.1")
                        .location("Hanoi, Vietnam (Local)")
                        .visitedAt(now.minusMinutes(15))
                        .build()
        ));

        String expectedDate = now.minusHours(2).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));

        // Login History tab
        String loginsHtml = mvc.perform(get("/admin/tracking")
                        .param("tab", "logins")
                        .with(user(new CustomUserDetails(admin))))
                .andExpect(status().isOk())
                .andExpect(model().attribute("activeTab", "logins"))
                .andReturn().getResponse().getContentAsString();

        assertThat(loginsHtml)
                .contains("Alice Reader", "alice@booknet.test", "Google OAuth2", "Singapore, Singapore",
                        "Book Catalog", "Most Visited Page", "2 views", "1 logins today",
                        "Chrome \u00b7 Windows", "Firefox \u00b7 Linux", expectedDate)
                .doesNotContain("th:style=", "th:classappend=", "<th>IP Address</th>", "203.0.113.5", "127.0.0.1", "thg");

        // Page Navigation Logs tab with role & keyword filter
        String visitsHtml = mvc.perform(get("/admin/tracking")
                        .param("tab", "visits")
                        .param("role", "GUEST")
                        .param("keyword", "catalog")
                        .param("from", LocalDate.now().minusDays(1).toString())
                        .param("to", LocalDate.now().toString())
                        .with(user(new CustomUserDetails(admin))))
                .andExpect(status().isOk())
                .andExpect(model().attribute("activeTab", "visits"))
                .andReturn().getResponse().getContentAsString();

        assertThat(visitsHtml)
                .contains("Anonymous (Guest)", "Book Catalog", "/catalog", "Page Visits by Location",
                        now.minusMinutes(15).format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
                .doesNotContain("alice@booknet.test", "<th>IP Address</th>", "127.0.0.1", "thg");
    }

    @Test
    void formatDeviceParsesCommonBrowserAndOsCombinations() {
        assertThat(AdminTrackingController.formatDevice(
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/130.0.0.0 Safari/537.36 Edg/130.0.0.0"))
                .isEqualTo("Edge \u00b7 Windows");
        assertThat(AdminTrackingController.formatDevice(
                "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 Version/18.0 Safari/605.1.15"))
                .isEqualTo("Safari \u00b7 macOS");
        assertThat(AdminTrackingController.formatDevice(
                "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 Chrome/130.0.0.0 Mobile Safari/537.36"))
                .isEqualTo("Chrome \u00b7 Android");
        assertThat(AdminTrackingController.formatDevice(
                "Mozilla/5.0 (iPhone; CPU iPhone OS 17_5 like Mac OS X) AppleWebKit/605.1.15 Mobile/15E148 Safari/604.1"))
                .isEqualTo("Safari \u00b7 iOS");
        assertThat(AdminTrackingController.formatDevice(null)).isEqualTo("Unknown");
        assertThat(AdminTrackingController.formatDevice("Unknown")).isEqualTo("Unknown");
    }

    @Test
    void pageInterceptorRecordsAuthenticatedAndGuestHtmlVisitsAndSkipsStaticResources() throws Exception {
        // Guest visit to /catalog
        mvc.perform(get("/catalog").header("Accept", "text/html"))
                .andExpect(status().isOk());

        // Member visit to /catalog
        mvc.perform(get("/catalog")
                        .header("Accept", "text/html")
                        .with(user(new CustomUserDetails(member))))
                .andExpect(status().isOk());

        // Static CSS should NOT be tracked
        mvc.perform(get("/css/base/tokens.css"))
                .andExpect(status().isOk());

        List<PageVisitLog> visits = pageVisitRepo.findAll();
        assertThat(visits).hasSize(2);
        assertThat(visits).anySatisfy(v -> {
            assertThat(v.getUserEmail()).isEqualTo("Anonymous (Guest)");
            assertThat(v.getUserRole()).isEqualTo("GUEST");
            assertThat(v.getPageName()).isEqualTo("Book Catalog");
            assertThat(v.getLocation()).isEqualTo("Hanoi, Vietnam (Local)");
        });
        assertThat(visits).anySatisfy(v -> {
            assertThat(v.getUserEmail()).isEqualTo(member.getEmail());
            assertThat(v.getUserRole()).isEqualTo("MEMBER");
            assertThat(v.getPageName()).isEqualTo("Book Catalog");
        });
        assertThat(visits).noneMatch(v -> v.getPagePath().contains("/css/"));
    }

    @Test
    void passwordLoginTriggersLoginSuccessListener() throws Exception {
        mvc.perform(post("/login")
                        .with(csrf())
                        .param("email", member.getEmail())
                        .param("password", RAW_PASSWORD)
                        .header("User-Agent", "BooknetTestBrowser/1.0"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));

        List<UserLoginLog> logs = loginLogRepo.findAll();
        assertThat(logs).singleElement().satisfies(logEntry -> {
            assertThat(logEntry.getUserEmail()).isEqualTo(member.getEmail());
            assertThat(logEntry.getUserName()).isEqualTo("Tracking Member");
            assertThat(logEntry.getUserRole()).isEqualTo("MEMBER");
            assertThat(logEntry.getLoginMethod()).isEqualTo("PASSWORD");
            assertThat(logEntry.getLocation()).isEqualTo("Hanoi, Vietnam (Local)");
            assertThat(logEntry.getUserAgent()).contains("BooknetTestBrowser/1.0");
        });
    }

    @Test
    void ipLocationResolverHandlesLocalAndForwardedHeadersSafely() {
        assertThat(ipLocationResolver.resolve(null)).isEqualTo("Hanoi, Vietnam (Local)");
        assertThat(ipLocationResolver.resolve("127.0.0.1")).isEqualTo("Hanoi, Vietnam (Local)");
        assertThat(ipLocationResolver.resolve("0:0:0:0:0:0:0:1")).isEqualTo("Hanoi, Vietnam (Local)");
        assertThat(ipLocationResolver.resolve("192.168.1.50")).isEqualTo("Hanoi, Vietnam (Local)");
        assertThat(ipLocationResolver.resolve("unknown")).isEqualTo("Hanoi, Vietnam (Local)");

        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("X-Forwarded-For", "10.0.0.5, 172.16.0.1");
        assertThat(ipLocationResolver.extractIp(req)).isEqualTo("10.0.0.5");

        MockHttpServletRequest reqUnknown = new MockHttpServletRequest();
        reqUnknown.addHeader("X-Forwarded-For", "unknown");
        reqUnknown.addHeader("X-Real-IP", "192.168.0.12");
        assertThat(ipLocationResolver.extractIp(reqUnknown)).isEqualTo("192.168.0.12");
    }

    @Test
    void oauth2LoginEventRecordsGoogleMethodAndUserEmail() {
        User googleUser = User.builder()
                .id(999L)
                .fullName("Google Tracking Reader")
                .email("google-track@booknet.test")
                .role(UserRole.ROLE_USER)
                .provider(AuthProvider.GOOGLE)
                .status(UserStatus.ACTIVE)
                .enabled(true)
                .emailVerified(true)
                .build();

        DefaultOAuth2User delegate = new DefaultOAuth2User(
                googleUser.effectiveAuthorities(),
                Map.of("sub", "google-sub-1", "email", "google-track@booknet.test", "name", "Google Tracking Reader"),
                "email"
        );
        CustomOAuth2User principal = new CustomOAuth2User(delegate, googleUser);
        OAuth2AuthenticationToken token = new OAuth2AuthenticationToken(
                principal, principal.getAuthorities(), "google"
        );

        loginSuccessListener.onLoginSuccess(new InteractiveAuthenticationSuccessEvent(token, getClass()));

        List<UserLoginLog> logs = loginLogRepo.findAll();
        assertThat(logs).singleElement().satisfies(logEntry -> {
            assertThat(logEntry.getUserEmail()).isEqualTo("google-track@booknet.test");
            assertThat(logEntry.getUserName()).isEqualTo("Google Tracking Reader");
            assertThat(logEntry.getUserRole()).isEqualTo("MEMBER");
            assertThat(logEntry.getLoginMethod()).isEqualTo("GOOGLE_OAUTH2");
            assertThat(logEntry.getLocation()).isEqualTo("Hanoi, Vietnam (Local)");
        });
    }
}
