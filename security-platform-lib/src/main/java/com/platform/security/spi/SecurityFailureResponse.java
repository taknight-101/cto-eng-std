package com.platform.security.spi;

import java.util.Map;

/** Framework-agnostic description of the response the servlet layer should write for a security failure. */
public record SecurityFailureResponse(int status, Map<String, Object> body, Map<String, String> headers) {

    public SecurityFailureResponse {
        body = body == null ? Map.of() : Map.copyOf(body);
        headers = headers == null ? Map.of() : Map.copyOf(headers);
    }

    public static SecurityFailureResponse of(int status, String message) {
        return new SecurityFailureResponse(status, Map.of("error", message), Map.of());
    }
}
