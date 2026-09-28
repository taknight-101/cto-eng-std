package com.platform.example.http;

import com.platform.http.api.HttpRequestSpec;
import com.platform.http.middleware.HttpMiddleware;
import com.platform.http.middleware.HttpMiddlewareChain;
import com.platform.http.middleware.HttpMiddlewareOutcome;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;

/**
 * Adds a client-credentials access token to outgoing requests. {@code http-client-lib} itself
 * never learns anything about Keycloak/OAuth2 - it only sees a middleware that adds an
 * {@code Authorization} header, obtained via Spring Security's own
 * {@link OAuth2AuthorizedClientManager} (which handles token caching/refresh).
 */
public final class ClientCredentialsAuthMiddleware implements HttpMiddleware {

    private final OAuth2AuthorizedClientManager clientManager;
    private final String registrationId;

    public ClientCredentialsAuthMiddleware(OAuth2AuthorizedClientManager clientManager, String registrationId) {
        this.clientManager = clientManager;
        this.registrationId = registrationId;
    }

    @Override
    public HttpMiddlewareOutcome handle(HttpRequestSpec request, HttpMiddlewareChain chain) throws Exception {
        OAuth2AuthorizeRequest authorizeRequest =
                OAuth2AuthorizeRequest.withClientRegistrationId(registrationId).principal(registrationId).build();
        OAuth2AuthorizedClient authorizedClient = clientManager.authorize(authorizeRequest);
        if (authorizedClient == null) {
            throw new IllegalStateException(
                    "Unable to obtain a client-credentials access token for '" + registrationId + "'");
        }
        String token = authorizedClient.getAccessToken().getTokenValue();
        return chain.proceed(request.withHeader("Authorization", "Bearer " + token));
    }

    @Override
    public String name() {
        return "service-auth";
    }
}
