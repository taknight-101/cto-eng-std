package com.platform.security.auth;

import com.platform.security.api.SecurityRequestContext;

import java.util.Optional;

/** Extracts a raw bearer token string from the request context; pluggable so custom transports can be supported. */
@FunctionalInterface
public interface BearerTokenExtractor {

    Optional<String> extract(SecurityRequestContext context);

    static BearerTokenExtractor authorizationHeader() {
        return context -> context.headers().entrySet().stream()
                .filter(entry -> entry.getKey().equalsIgnoreCase("Authorization"))
                .map(entry -> entry.getValue())
                .filter(value -> value != null && value.regionMatches(true, 0, "Bearer ", 0, 7))
                .map(value -> value.substring(7).trim())
                .filter(token -> !token.isEmpty())
                .findFirst();
    }
}
