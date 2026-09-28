package com.platform.example.config;

import com.platform.security.api.SecurityEventDrill;
import com.platform.security.api.SecurityMiddleware;
import com.platform.security.api.SecurityMiddlewareRegistry;
import com.platform.security.api.SecurityPipeline;
import com.platform.security.api.SecurityPipelineStages;
import com.platform.security.spi.SecurityFailureHandler;
import com.platform.security.spring.SecurityPlatformConfigurer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Two filter chains, in order: {@code /dev/**} and {@code /internal/**} stay outside the
 * security platform entirely (see {@link com.platform.example.web.DevTokenController} and
 * {@link com.platform.example.web.InventoryController} javadoc for why), while {@code /api/**}
 * is wired through {@link SecurityPlatformConfigurer}. This is the standard Spring Security
 * pattern for excluding paths from a filter, not a platform-specific mechanism.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final Logger log = LoggerFactory.getLogger("com.platform.example.audit");

    @Order(1)
    @Bean
    public SecurityFilterChain publicFilterChain(HttpSecurity http) throws Exception {
        http.securityMatcher("/dev/**", "/internal/**", "/actuator/**")
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }

    @Order(2)
    @Bean
    public SecurityFilterChain apiFilterChain(
            HttpSecurity http,
            SecurityPipeline pipeline,
            SecurityEventDrill eventDrill,
            SecurityFailureHandler failureHandler) throws Exception {
        http.securityMatcher("/api/**")
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .with(SecurityPlatformConfigurer.securityPlatform(pipeline, eventDrill, failureHandler),
                        Customizer.withDefaults())
                // The platform's own AuthenticationMiddleware/AuthorizationMiddleware already
                // decided authn/authz by the time Spring Security's authorization stage runs.
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }

    /** Registers a demo "audit" middleware ahead of authentication, logging every /api/** request. */
    @Bean
    public org.springframework.boot.ApplicationRunner registerAuditMiddleware(SecurityMiddlewareRegistry registry) {
        return args -> registry.registerBefore(SecurityPipelineStages.AUTHENTICATION, "audit",
                SecurityMiddleware.named("audit")
                        .before(context -> {
                            log.info("[audit] {} {} correlationId={}",
                                    context.requestMethod(), context.requestPath(), context.correlationId());
                            return context;
                        })
                        .after(context -> {
                            log.info("[audit] completed phase={} correlationId={}", context.phase(), context.correlationId());
                            return context;
                        })
                        .build());
    }
}
