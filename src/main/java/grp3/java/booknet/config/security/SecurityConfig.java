package grp3.java.booknet.config.security;

import grp3.java.booknet.security.service.CustomOAuth2UserService;
import grp3.java.booknet.security.service.CustomUserDetailsService;
import grp3.java.booknet.security.service.CustomOidcUserService;
import grp3.java.booknet.security.session.AccountSessionFilter;
import grp3.java.booknet.security.navigation.LoginDestination;
import grp3.java.booknet.repository.user.UserRepository;
import grp3.java.booknet.enums.user.UserRole;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.rememberme.RememberMeAuthenticationFilter;
import org.springframework.security.web.util.matcher.RequestMatcher;

import java.io.IOException;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService customUserDetailsService;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final CustomOidcUserService customOidcUserService;
    private final UserRepository userRepository;

    @Value("${BOOKNET_REMEMBER_ME_KEY:}")
    private String configuredRememberMeKey;
    private final String ephemeralRememberMeKey = java.util.UUID.randomUUID().toString();

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(customUserDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        RequestMatcher apiRequest = request -> request.getRequestURI().equals(request.getContextPath() + "/api")
                || request.getRequestURI().startsWith(request.getContextPath() + "/api/");
        http
            .authenticationProvider(authenticationProvider())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/",
                    "/home",
                    "/catalog",
                    "/about",
                    "/contact",
                    "/login",
                    "/register",
                    "/forgot-password", "/reset-password", "/verify-email", "/verification/resend",
                    "/css/**",
                    "/js/**",
                    "/images/**", "/fonts/**",
                    "/favicon.ico",
                    "/oauth2/**"
                ).permitAll()
                .requestMatchers("/staff/loans", "/staff/loans/**", "/librarian/loans", "/librarian/loans/**", "/admin/loans", "/admin/loans/**").hasAuthority("CIRCULATION")
                .requestMatchers("/staff/reservations", "/staff/reservations/**", "/librarian/reservations", "/librarian/reservations/**", "/admin/reservations", "/admin/reservations/**").hasAuthority("CIRCULATION")
                .requestMatchers("/staff/notifications/email", "/staff/notifications/email/**", "/librarian/notifications/email", "/librarian/notifications/email/**", "/admin/notifications/email", "/admin/notifications/email/**").hasAuthority("SERVICE_EMAIL")
                .requestMatchers("/member/loans/*/return-detail").hasAnyAuthority("ROLE_MEMBER", "MEMBER_READ", "CIRCULATION")
                .requestMatchers("/member/loans/*/return/confirm").hasAuthority("CIRCULATION")
                .requestMatchers("/admin/books", "/admin/books/**").hasAnyAuthority("CATALOG_EDIT", "COPY_MANAGE", "BOOK_RETIRE")
                .requestMatchers("/admin/**", "/api/admin/**").hasRole("ADMIN")
                .requestMatchers("/staff/**").hasAuthority("MEMBER_READ")
                .requestMatchers("/api/staff/**").hasAuthority("MEMBER_READ")
                .requestMatchers("/member/**", "/api/member/**").hasRole("MEMBER")
                .requestMatchers("/account/**", "/api/account/**").hasAnyRole("MEMBER", "LIBRARIAN", "ADMIN", "CUSTOM")
                .anyRequest().authenticated()
            )
            .exceptionHandling(errors -> errors
                .defaultAuthenticationEntryPointFor((request, response, exception) -> {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"error\":\"Authentication required\"}");
                }, apiRequest)
                .defaultAuthenticationEntryPointFor(new LoginUrlAuthenticationEntryPoint("/login"), request -> true)
            )
            .anonymous(anonymous -> anonymous.authorities(UserRole.ROLE_GUEST.name()))
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .usernameParameter("email")
                .passwordParameter("password")
                .successHandler(SecurityConfig::redirectAfterAuthentication)
                .failureUrl("/login?error=true")
                .permitAll()
            )
            .oauth2Login(oauth2 -> oauth2
                .loginPage("/login")
                .userInfoEndpoint(userInfo -> userInfo
                    .userService(customOAuth2UserService)
                    .oidcUserService(customOidcUserService)
                )
                .successHandler((request, response, authentication) -> {
                    HttpSession session = request.getSession(false);
                    boolean linkingSucceeded = session != null
                            && Boolean.TRUE.equals(session.getAttribute(OAuthLinkSessionAttributes.GOOGLE_LINK_SUCCEEDED));
                    clearGoogleLinkSession(session);
                    String target = linkingSucceeded ? "/?google_linked=true" : landingPage(authentication);
                    response.sendRedirect(request.getContextPath() + target);
                })
                .failureHandler((request, response, exception) -> {
                    HttpSession session = request.getSession(false);
                    boolean wasLinking = session != null
                            && session.getAttribute(OAuthLinkSessionAttributes.GOOGLE_LINK_USER_ID) != null;
                    clearGoogleLinkSession(session);
                    response.sendRedirect(wasLinking ? "/?google_link_error=true" : "/login?oauth2_error=true");
                })
            )
            .addFilterAfter(new AccountSessionFilter(userRepository), RememberMeAuthenticationFilter.class)
            .rememberMe(remember -> remember
                .rememberMeServices(new grp3.java.booknet.security.session.VersionedRememberMeServices(
                        configuredRememberMeKey.isBlank() ? ephemeralRememberMeKey : configuredRememberMeKey,
                        customUserDetailsService, userRepository))
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout=true")
                .invalidateHttpSession(true)
                .clearAuthentication(true)
                .deleteCookies("JSESSIONID", "remember-me")
                .permitAll()
            );

        return http.build();
    }

    private static void redirectAfterAuthentication(HttpServletRequest request,
                                                    HttpServletResponse response,
                                                    Authentication authentication) throws IOException {
        response.sendRedirect(request.getContextPath() + landingPage(authentication));
    }

    private static String landingPage(Authentication authentication) {
        return LoginDestination.forAuthentication(authentication);
    }

    private void clearGoogleLinkSession(HttpSession session) {
        if (session == null) {
            return;
        }
        session.removeAttribute(OAuthLinkSessionAttributes.GOOGLE_LINK_USER_ID);
        session.removeAttribute(OAuthLinkSessionAttributes.GOOGLE_LINK_STARTED_AT);
        session.removeAttribute(OAuthLinkSessionAttributes.GOOGLE_LINK_SUCCEEDED);
    }
}
