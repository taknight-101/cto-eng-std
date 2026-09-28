package com.platform.security.api;

/**
 * Specialization marker for authenticators that adapt Spring Security's OAuth2 resource-server
 * machinery (already installed earlier in the filter chain) into the platform's pipeline,
 * rather than reimplementing OAuth2 token introspection/validation.
 */
public interface OAuth2Authenticator extends TokenAuthenticator {
}
