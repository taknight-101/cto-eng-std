package com.platform.security.api;

import java.util.Map;
import java.util.Objects;

/**
 * Library-owned representation of an authenticated principal, independent of any particular
 * Spring Security {@code Authentication} implementation.
 *
 * @param name       principal identifier (typically the JWT {@code sub} claim or username)
 * @param attributes additional, immutable, principal-scoped attributes (e.g. claims)
 */
public record SecurityPrincipal(String name, Map<String, Object> attributes) {

    public SecurityPrincipal {
        Objects.requireNonNull(name, "name must not be null");
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
    }

    public static SecurityPrincipal of(String name) {
        return new SecurityPrincipal(name, Map.of());
    }

    public static SecurityPrincipal of(String name, Map<String, Object> attributes) {
        return new SecurityPrincipal(name, attributes);
    }
}
