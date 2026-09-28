package com.platform.http.middleware;

import com.platform.http.api.HttpRequestSpec;

/** Represents the remainder of the middleware chain, terminating in dispatch to the downstream call. */
@FunctionalInterface
public interface HttpMiddlewareChain {

    HttpMiddlewareOutcome proceed(HttpRequestSpec request) throws Exception;
}
