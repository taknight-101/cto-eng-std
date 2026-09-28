package com.platform.http.api;

import java.util.Map;

/** Thrown (optionally - see {@code HttpClient} configuration) when a downstream call returns a 4xx/5xx status. */
public class DownstreamErrorException extends HttpClientPlatformException {

    private final transient HttpResponse<?> response;

    public DownstreamErrorException(String message, HttpResponse<?> response) {
        super(message, null,
                response.isServerError() ? HttpErrorCode.DOWNSTREAM_SERVER_ERROR : HttpErrorCode.DOWNSTREAM_CLIENT_ERROR,
                "downstream", Map.of("status", response.status()));
        this.response = response;
    }

    public HttpResponse<?> response() {
        return response;
    }
}
