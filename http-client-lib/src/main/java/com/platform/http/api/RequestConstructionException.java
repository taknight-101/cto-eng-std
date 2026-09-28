package com.platform.http.api;

import java.util.Map;

/** Thrown when an {@link HttpRequestSpec} cannot be built or serialized before being sent. */
public class RequestConstructionException extends HttpClientPlatformException {

    public RequestConstructionException(String message, Throwable cause) {
        super(message, cause, HttpErrorCode.REQUEST_CONSTRUCTION_FAILED, "request-construction", Map.of());
    }
}
