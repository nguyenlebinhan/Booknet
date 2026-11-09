package grp3.java.booknet.config;

import grp3.java.booknet.security.CustomOAuth2UserService;
import grp3.java.booknet.security.CustomUserDetailsService;
import grp3.java.booknet.security.OAuthLinkSessionAttributes;
import grp3.java.booknet.security.CustomOidcUserService;
import grp3.java.booknet.security.AccountSessionFilter;
import grp3.java.booknet.repository.user.UserRepository;
import grp3.java.booknet.enums.user.UserRole;
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
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.rememberme.RememberMeAuthenticationFilter;

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
        http
            .authenticationProvider(authenticationProvider())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/",
                    "/home",
                    "/login",
                    "/register",
                    "/forgot-password", "/reset-password", "/verify-email", "/verification/resend",
                    "/css/**",
                    "/js/**",
                    "/images/**",
                    "/favicon.ico",
                    "/oauth2/**",
                    "/api/auth/csrf", "/api/auth/register", "/api/auth/forgot-password",
                    "/api/auth/reset-password", "/api/auth/verify-email", "/api/auth/verification/resend"
                ).permitAll()
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .anonymous(anonymous -> anonymous.authorities(UserRole.ROLE_GUEST.name()))
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .usernameParameter("email")
                .passwordParameter("password")
                .defaultSuccessUrl("/", true)
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
                    response.sendRedirect(linkingSucceeded ? "/?google_linked=true" : "/");
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
            .exceptionHandling(errors -> errors
                .defaultAuthenticationEntryPointFor((request, response, exception) -> {
                    response.setStatus(401);
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write("{\"message\":\"Vui lòng đăng nhập.\"}");
                }, request -> request.getRequestURI().startsWith(request.getContextPath() + "/api/"))
            )
            .rememberMe(remember -> remember
                .key(configuredRememberMeKey.isBlank() ? ephemeralRememberMeKey : configuredRememberMeKey)
                .tokenValiditySeconds(7 * 24 * 60 * 60)
                .rememberMeParameter("remember-me")
                .userDetailsService(customUserDetailsService)
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

    private void clearGoogleLinkSession(HttpSession session) {
        if (session == null) {
            return;
        }
        session.removeAttribute(OAuthLinkSessionAttributes.GOOGLE_LINK_USER_ID);
        session.removeAttribute(OAuthLinkSessionAttributes.GOOGLE_LINK_STARTED_AT);
        session.removeAttribute(OAuthLinkSessionAttributes.GOOGLE_LINK_SUCCEEDED);
    }
}
