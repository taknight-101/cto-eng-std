package com.platform.security.autoconfigure;

import com.platform.security.api.AuthorizationPolicy;
import com.platform.security.api.JwtAuthenticator;
import com.platform.security.api.OAuth2Authenticator;
import com.platform.security.api.SecurityContextAccessor;
import com.platform.security.api.SecurityEventDrill;
import com.platform.security.api.SecurityMiddlewareRegistry;
import com.platform.security.api.SecurityPipeline;
import com.platform.security.api.SecurityPipelineStages;
import com.platform.security.api.TokenAuthenticator;
import com.platform.security.auth.AuthenticationMiddleware;
import com.platform.security.auth.BearerTokenExtractor;
import com.platform.security.auth.DefaultJwtAuthenticator;
import com.platform.security.auth.DelegatingJwtValidator;
import com.platform.security.auth.JwtAuthorityMappers;
import com.platform.security.auth.JwtClaimsSet;
import com.platform.security.auth.SpringOAuth2Authenticator;
import com.platform.security.authorization.AuthorizationMiddleware;
import com.platform.security.authorization.AuthorizationPolicies;
import com.platform.security.events.DefaultSecurityEventDrill;
import com.platform.security.events.LoggingSecurityEventConsumer;
import com.platform.security.pipeline.DefaultSecurityMiddlewareRegistry;
import com.platform.security.pipeline.DefaultSecurityPipeline;
import com.platform.security.spi.SecurityFailureHandler;
import com.platform.security.spring.DefaultSecurityFailureHandler;
import com.platform.security.spring.SpringSecurityContextAccessor;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

/**
 * Wires the security platform beans. Deliberately never registers a {@code SecurityFilterChain}
 * itself - see {@code SecurityPlatformConfigurer} for how applications opt in explicitly.
 */
@AutoConfiguration
@EnableConfigurationProperties(SecurityPlatformProperties.class)
@ConditionalOnProperty(prefix = "security-platform", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SecurityPlatformAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public SecurityMiddlewareRegistry securityMiddlewareRegistry() {
        return new DefaultSecurityMiddlewareRegistry();
    }

    @Bean
    @ConditionalOnMissingBean
    public SecurityPipeline securityPipeline(SecurityMiddlewareRegistry registry) {
        return new DefaultSecurityPipeline(registry);
    }

    @Bean
    @ConditionalOnMissingBean
    public SecurityEventDrill securityEventDrill() {
        return new DefaultSecurityEventDrill();
    }

    @Bean
    @ConditionalOnMissingBean
    public SecurityContextAccessor securityContextAccessor() {
        return new SpringSecurityContextAccessor();
    }

    @Bean
    @ConditionalOnMissingBean
    public SecurityFailureHandler securityFailureHandler() {
        return new DefaultSecurityFailureHandler();
    }

    @Bean
    @ConditionalOnMissingBean
    public AuthorizationPolicy authorizationPolicy() {
        return AuthorizationPolicies.permitAll();
    }

    @Bean
    @ConditionalOnMissingBean(JwtDecoder.class)
    @ConditionalOnProperty(prefix = "security-platform.jwt", name = "enabled", havingValue = "true")
    public JwtDecoder jwtDecoder(SecurityPlatformProperties properties) {
        SecurityPlatformProperties.Jwt jwtProps = properties.getJwt();
        NimbusJwtDecoder decoder;
        if (jwtProps.getJwkSetUri() != null && !jwtProps.getJwkSetUri().isBlank()) {
            decoder = NimbusJwtDecoder.withJwkSetUri(jwtProps.getJwkSetUri()).build();
        } else if (jwtProps.getHmacSecret() != null && !jwtProps.getHmacSecret().isBlank()) {
            SecretKeySpec key = new SecretKeySpec(
                    jwtProps.getHmacSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            decoder = NimbusJwtDecoder.withSecretKey(key).build();
        } else {
            throw new IllegalStateException(
                    "security-platform.jwt.enabled=true requires either jwk-set-uri or hmac-secret (demo only)");
        }
        decoder.setJwtValidator(buildValidator(jwtProps));
        return decoder;
    }

    /** Composes the default timestamp/issuer validator with a real audience check when {@code audience} is set. */
    private static OAuth2TokenValidator<Jwt> buildValidator(SecurityPlatformProperties.Jwt jwtProps) {
        OAuth2TokenValidator<Jwt> timestampAndIssuer = jwtProps.getIssuer() == null || jwtProps.getIssuer().isBlank()
                ? JwtValidators.createDefault()
                : JwtValidators.createDefaultWithIssuer(jwtProps.getIssuer());
        if (jwtProps.getAudience() == null || jwtProps.getAudience().isBlank()) {
            return timestampAndIssuer;
        }
        OAuth2TokenValidator<Jwt> audienceValidator = jwt -> jwt.getAudience() != null
                && jwt.getAudience().contains(jwtProps.getAudience())
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token",
                        "Required audience '" + jwtProps.getAudience() + "' is missing from the token", null));
        return new DelegatingOAuth2TokenValidator<>(timestampAndIssuer, audienceValidator);
    }

    @Bean
    @ConditionalOnMissingBean(JwtAuthenticator.class)
    @ConditionalOnProperty(prefix = "security-platform.jwt", name = "enabled", havingValue = "true")
    public JwtAuthenticator jwtAuthenticator(JwtDecoder jwtDecoder, SecurityPlatformProperties properties) {
        SecurityPlatformProperties.Jwt jwtProps = properties.getJwt();
        Function<JwtClaimsSet, Set<String>> mapper =
                JwtAuthorityMappers.fromClaim(jwtProps.getAuthorityClaim(), jwtProps.getAuthorityPrefix());
        return new DefaultJwtAuthenticator(
                BearerTokenExtractor.authorizationHeader(), new DelegatingJwtValidator(jwtDecoder), mapper);
    }

    @Bean
    @ConditionalOnMissingBean(OAuth2Authenticator.class)
    @ConditionalOnProperty(prefix = "security-platform.oauth2", name = "enabled", havingValue = "true")
    public OAuth2Authenticator oAuth2Authenticator() {
        return new SpringOAuth2Authenticator();
    }

    @Bean
    public SecurityPipelineInitializer securityPipelineInitializer(
            SecurityMiddlewareRegistry registry,
            SecurityEventDrill eventDrill,
            AuthorizationPolicy authorizationPolicy,
            ObjectProvider<TokenAuthenticator> authenticators,
            SecurityPlatformProperties properties) {
        return new SecurityPipelineInitializer(
                registry, eventDrill, authorizationPolicy, authenticators.orderedStream().toList(), properties);
    }

    /** Registers the built-in authentication/authorization middleware once all authenticator beans exist. */
    static final class SecurityPipelineInitializer implements InitializingBean {

        private final SecurityMiddlewareRegistry registry;
        private final SecurityEventDrill eventDrill;
        private final AuthorizationPolicy authorizationPolicy;
        private final List<TokenAuthenticator> authenticators;
        private final SecurityPlatformProperties properties;

        SecurityPipelineInitializer(
                SecurityMiddlewareRegistry registry,
                SecurityEventDrill eventDrill,
                AuthorizationPolicy authorizationPolicy,
                List<TokenAuthenticator> authenticators,
                SecurityPlatformProperties properties) {
            this.registry = registry;
            this.eventDrill = eventDrill;
            this.authorizationPolicy = authorizationPolicy;
            this.authenticators = authenticators;
            this.properties = properties;
        }

        @Override
        public void afterPropertiesSet() {
            if (properties.getEvents().isLoggingConsumerEnabled()) {
                eventDrill.subscribe(new LoggingSecurityEventConsumer());
            }
            registry.register(SecurityPipelineStages.AUTHENTICATION,
                    new AuthenticationMiddleware(authenticators, eventDrill, properties.isAuthenticationRequired()),
                    SecurityPipelineStages.AUTHENTICATION_ORDER);
            registry.register(SecurityPipelineStages.AUTHORIZATION,
                    new AuthorizationMiddleware(authorizationPolicy, eventDrill),
                    SecurityPipelineStages.AUTHORIZATION_ORDER);
        }
    }
}
