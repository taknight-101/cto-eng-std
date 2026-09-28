package com.platform.http.spring;

import com.platform.http.api.HttpRequestSpec;

/**
 * Internal adapter boundary: the only interface {@code DefaultHttpClient} depends on for
 * actually sending bytes over the network. The public API never depends on this type directly
 * - {@link WebClientHttpEngine} is the only implementation, keeping WebClient out of the
 * public surface.
 */
public interface HttpEngine {

    RawHttpResponse execute(HttpRequestSpec request);
}
