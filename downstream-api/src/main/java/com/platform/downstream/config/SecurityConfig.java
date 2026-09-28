package com.platform.downstream.config;

import com.platform.security.api.SecurityEventDrill;
import com.platform.security.api.SecurityPipeline;
import com.platform.security.spi.SecurityFailureHandler;
import com.platform.security.spring.SecurityPlatformConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Two filter chains: {@code /actuator/**} (health checks, unauthenticated - used by the Docker
 * Compose healthcheck) stays outside the security platform, while every real endpoint under
 * {@code /internal/**} is protected by the platform's own pipeline (JWT authentication +
 * {@code ROLE_DOWNSTREAM_ACCESS} authorization). There is no other public/dev bypass in this
 * service, unlike spring-api-example.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Order(1)
    @Bean
    public SecurityFilterChain actuatorFilterChain(HttpSecurity http) throws Exception {
        http.securityMatcher("/actuator/**")
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
        http.securityMatcher("/internal/**")
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .with(SecurityPlatformConfigurer.securityPlatform(pipeline, eventDrill, failureHandler),
                        Customizer.withDefaults())
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }
}
