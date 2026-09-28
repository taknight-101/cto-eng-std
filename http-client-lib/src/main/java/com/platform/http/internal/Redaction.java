package com.platform.http.internal;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Prevents sensitive header/metadata values from reaching logs, events or trace metadata. */
public final class Redaction {

    private static final String MASK = "***REDACTED***";
    private static final Set<String> SENSITIVE_HEADER_NAMES =
            Set.of("authorization", "proxy-authorization", "cookie", "set-cookie", "x-api-key");

    private Redaction() {
    }

    public static Map<String, List<String>> sanitizeHeaders(Map<String, List<String>> headers) {
        Map<String, List<String>> sanitized = new LinkedHashMap<>();
        headers.forEach((name, values) -> sanitized.put(
                name, SENSITIVE_HEADER_NAMES.contains(name.toLowerCase(Locale.ROOT)) ? List.of(MASK) : values));
        return sanitized;
    }
}
