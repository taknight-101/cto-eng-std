package com.platform.security.internal;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Prevents sensitive values (tokens, secrets, credentials) from reaching logs, events, trace
 * metadata or {@code toString()} output.
 */
public final class Redaction {

    private static final String MASK = "***REDACTED***";

    private static final Set<String> SENSITIVE_HEADER_NAMES = Set.of(
            "authorization", "proxy-authorization", "cookie", "set-cookie");

    private static final Set<String> SENSITIVE_KEY_FRAGMENTS = Set.of(
            "password", "token", "secret", "credential", "authorization", "apikey", "api-key", "privatekey");

    private Redaction() {
    }

    public static Map<String, String> sanitizeHeaders(Map<String, String> headers) {
        Map<String, String> sanitized = new LinkedHashMap<>();
        headers.forEach((name, value) -> sanitized.put(
                name, SENSITIVE_HEADER_NAMES.contains(name.toLowerCase(Locale.ROOT)) ? MASK : value));
        return sanitized;
    }

    public static Map<String, Object> sanitizeMetadata(Map<String, Object> metadata) {
        Map<String, Object> sanitized = new LinkedHashMap<>();
        metadata.forEach((key, value) -> sanitized.put(key, isSensitiveKey(key) ? MASK : value));
        return sanitized;
    }

    public static String sanitizeToken(String token) {
        if (token == null || token.isBlank()) {
            return token;
        }
        return MASK;
    }

    private static boolean isSensitiveKey(String key) {
        String lower = key.toLowerCase(Locale.ROOT);
        return SENSITIVE_KEY_FRAGMENTS.stream().anyMatch(lower::contains);
    }
}
