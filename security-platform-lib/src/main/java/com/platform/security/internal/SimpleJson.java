package com.platform.security.internal;

import java.util.Map;
import java.util.stream.Collectors;

/** Minimal JSON object writer for security failure response bodies, avoiding a hard Jackson dependency in this library. */
public final class SimpleJson {

    private SimpleJson() {
    }

    public static String toJson(Map<String, Object> values) {
        return values.entrySet().stream()
                .map(entry -> quote(entry.getKey()) + ":" + valueToJson(entry.getValue()))
                .collect(Collectors.joining(",", "{", "}"));
    }

    private static String valueToJson(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof Number || value instanceof Boolean) {
            return value.toString();
        }
        return quote(value.toString());
    }

    private static String quote(String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }
}
