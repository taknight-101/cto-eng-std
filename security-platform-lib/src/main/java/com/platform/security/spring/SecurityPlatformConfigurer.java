package com.platform.security.spring;

import com.platform.security.api.SecurityEventDrill;
import com.platform.security.api.SecurityPipeline;
import com.platform.security.spi.SecurityFailureHandler;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * {@code HttpSecurity} DSL entry point. Applications wire this into their own {@code
 * SecurityFilterChain} bean:
 *
 * <pre>{@code
 * http.with(SecurityPlatformConfigurer.securityPlatform(pipeline, eventDrill, failureHandler),
 *         Customizer.withDefaults());
 * }</pre>
 *
 * <p>This library never registers its own {@code SecurityFilterChain}; doing so would silently
 * override or conflict with the application's own security configuration.
 */
public final class SecurityPlatformConfigurer
        extends AbstractHttpConfigurer<SecurityPlatformConfigurer, HttpSecurity> {

    private final SecurityPipeline pipeline;
    private final SecurityEventDrill eventDrill;
    private final SecurityFailureHandler failureHandler;

    public SecurityPlatformConfigurer(
            SecurityPipeline pipeline, SecurityEventDrill eventDrill, SecurityFailureHandler failureHandler) {
        this.pipeline = pipeline;
        this.eventDrill = eventDrill;
        this.failureHandler = failureHandler;
    }

    public static SecurityPlatformConfigurer securityPlatform(
            SecurityPipeline pipeline, SecurityEventDrill eventDrill, SecurityFailureHandler failureHandler) {
        return new SecurityPlatformConfigurer(pipeline, eventDrill, failureHandler);
    }

    @Override
    public void configure(HttpSecurity http) {
        SecurityMiddlewareFilter filter = new SecurityMiddlewareFilter(pipeline, eventDrill, failureHandler);
        http.addFilterBefore(filter, UsernamePasswordAuthenticationFilter.class);
    }
}
