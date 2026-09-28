package com.platform.downstream.security;

import com.platform.security.api.AuthorizationPolicy;
import com.platform.security.authorization.AuthorizationPolicies;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Every request into this service requires the service-to-service {@code DOWNSTREAM_ACCESS} role. */
@Configuration
public class DownstreamAuthorizationPolicyConfig {

    @Bean
    public AuthorizationPolicy authorizationPolicy() {
        return AuthorizationPolicies.requireAuthority("ROLE_DOWNSTREAM_ACCESS");
    }
}
