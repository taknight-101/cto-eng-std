package com.platform.http.middleware;

import com.platform.http.api.HttpClientPlatformException;
import com.platform.http.api.HttpErrorCode;
import java.util.Map;

/** Wraps an unexpected exception thrown by user-registered {@link HttpMiddleware}. */
public class HttpMiddlewareException extends HttpClientPlatformException {

    public HttpMiddlewareException(String message, Throwable cause, String middlewareName) {
        super(message, cause, HttpErrorCode.MIDDLEWARE_FAILURE, "middleware", Map.of("middleware", middlewareName));
    }
}
