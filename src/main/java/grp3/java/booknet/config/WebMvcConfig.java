package grp3.java.booknet.config;

import grp3.java.booknet.config.tracking.PageTrackingInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * MVC configuration for registering custom interceptors.
 * Registers PageTrackingInterceptor for all page routes,
 * excluding static resources already handled by ResourceHttpRequestHandler.
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final PageTrackingInterceptor pageTrackingInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(pageTrackingInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/css/**", "/js/**", "/images/**", "/fonts/**",
                        "/webjars/**", "/favicon.ico", "/error", "/covers/**"
                );
    }
}
