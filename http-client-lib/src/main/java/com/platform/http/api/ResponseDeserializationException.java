package com.platform.http.api;

import java.util.Map;

/** Thrown when the response body cannot be deserialized into the requested type. */
public class ResponseDeserializationException extends HttpClientPlatformException {

    public ResponseDeserializationException(String message, Throwable cause) {
        super(message, cause, HttpErrorCode.DESERIALIZATION_FAILED, "deserialization", Map.of());
    }
}
