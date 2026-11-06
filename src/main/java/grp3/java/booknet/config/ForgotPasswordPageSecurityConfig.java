package grp3.java.booknet.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/** Opens only /forgot-password. Can be replaced by adding the path to permitAll in SecurityConfig. */
@Configuration
public class ForgotPasswordPageSecurityConfig {

    @Bean
    @Order(-1)
    public SecurityFilterChain forgotPasswordPageFilterChain(HttpSecurity http) throws Exception {
        return http
            .securityMatcher("/forgot-password")
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
            .build();
    }
}
