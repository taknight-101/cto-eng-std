package com.platform.security.auth;

import com.platform.security.api.AuthenticationOutcome;
import com.platform.security.api.OAuth2Authenticator;
import com.platform.security.api.SecurityRequestContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Adapts an {@code Authentication} already populated by Spring Security's own OAuth2
 * resource-server filter (which must run earlier in the filter chain) into the platform
 * pipeline, rather than reimplementing OAuth2 token introspection/validation.
 */
public final class SpringOAuth2Authenticator implements OAuth2Authenticator {

    @Override
    public AuthenticationOutcome authenticate(SecurityRequestContext context) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || isAnonymous(authentication)) {
            return AuthenticationOutcome.notApplicable();
        }
        return AuthenticationOutcome.success(authentication);
    }

    private static boolean isAnonymous(Authentication authentication) {
        return "anonymousUser".equals(authentication.getPrincipal());
    }
}
