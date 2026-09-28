package com.platform.security.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Typed configuration for {@code security-platform-lib}, bound from the {@code security-platform} prefix. */
@ConfigurationProperties(prefix = "security-platform")
public class SecurityPlatformProperties {

    /** Master on/off switch for the whole auto-configuration. */
    private boolean enabled = true;

    /** Whether a request with no applicable credentials should be rejected (401) by the built-in authentication middleware. */
    private boolean authenticationRequired = true;

    private final Jwt jwt = new Jwt();
    private final OAuth2 oauth2 = new OAuth2();
    private final Events events = new Events();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isAuthenticationRequired() {
        return authenticationRequired;
    }

    public void setAuthenticationRequired(boolean authenticationRequired) {
        this.authenticationRequired = authenticationRequired;
    }

    public Jwt getJwt() {
        return jwt;
    }

    public OAuth2 getOauth2() {
        return oauth2;
    }

    public Events getEvents() {
        return events;
    }

    public static class Jwt {
        private boolean enabled = false;
        private String jwkSetUri;
        private String hmacSecret;
        private String issuer;
        private String audience;
        private String authorityClaim = "roles";
        private String authorityPrefix = "ROLE_";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getJwkSetUri() {
            return jwkSetUri;
        }

        public void setJwkSetUri(String jwkSetUri) {
            this.jwkSetUri = jwkSetUri;
        }

        public String getHmacSecret() {
            return hmacSecret;
        }

        public void setHmacSecret(String hmacSecret) {
            this.hmacSecret = hmacSecret;
        }

        public String getIssuer() {
            return issuer;
        }

        public void setIssuer(String issuer) {
            this.issuer = issuer;
        }

        public String getAudience() {
            return audience;
        }

        public void setAudience(String audience) {
            this.audience = audience;
        }

        public String getAuthorityClaim() {
            return authorityClaim;
        }

        public void setAuthorityClaim(String authorityClaim) {
            this.authorityClaim = authorityClaim;
        }

        public String getAuthorityPrefix() {
            return authorityPrefix;
        }

        public void setAuthorityPrefix(String authorityPrefix) {
            this.authorityPrefix = authorityPrefix;
        }
    }

    public static class OAuth2 {
        private boolean enabled = false;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

    public static class Events {
        private boolean loggingConsumerEnabled = true;

        public boolean isLoggingConsumerEnabled() {
            return loggingConsumerEnabled;
        }

        public void setLoggingConsumerEnabled(boolean loggingConsumerEnabled) {
            this.loggingConsumerEnabled = loggingConsumerEnabled;
        }
    }
}
