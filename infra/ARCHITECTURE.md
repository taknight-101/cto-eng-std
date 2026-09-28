# Architecture: OAuth2/OIDC Integration

```
Browser / curl / smoke-test.sh
      |
      v
  Keycloak (OAuth2 Authorization Server / OIDC Provider)
      |
   issues a JWT (OAuth2 "token acquisition")
      |
      v
  spring-api-example  (Spring Boot, OAuth2 Resource Server)
      |
  security-platform-lib   <-- orchestration layer, NOT a replacement for Spring Security/Keycloak
      |  (SecurityMiddlewareFilter -> SecurityPipeline -> AuthenticationMiddleware ->
      |   AuthorizationMiddleware -> SecurityContextAccessor -> controller)
      v
  http-client-lib
      |  (HttpRequestSpec -> HttpMiddleware chain -> retry-aware dispatch -> HttpResponse,
      |   with a ClientCredentialsAuthMiddleware attaching a *second*, service-to-service
      |   OAuth2 token obtained independently from Keycloak)
      v
  downstream-api  (Spring Boot, ALSO an OAuth2 Resource Server, built on common-lib too)
```

## The central distinction: Keycloak vs. security-platform-lib

**Keycloak** is the identity provider. It is the only component that:

- Authenticates end users (verifies `alice`'s password) and issues user tokens.
- Authenticates the `service-client` (verifies its client secret) and issues service tokens.
- Owns the signing keys and exposes them via JWKS.
- Is the source of truth for realm roles.

**`security-platform-lib`** never authenticates anyone and never issues or signs a token. Its
job is entirely orchestration on top of Spring Security:

- Extracting the bearer token from the request (`BearerTokenExtractor`).
- Delegating _verification_ of a token it was handed to Spring Security's own `JwtDecoder`
  (backed by Nimbus JOSE+JWT) - it does not implement cryptography.
- Running that as one step (`AuthenticationMiddleware`) in a pluggable, observable pipeline.
- Evaluating an `AuthorizationPolicy` against the resulting authorities.
- Publishing every stage as a `SecurityEvent` so failures are observable without coupling
  application code to Spring Security's internal event types.

If you removed `security-platform-lib` entirely, Keycloak and Spring Security's OAuth2
resource-server support would still work on their own (Spring Security already knows how to
validate a JWT against an issuer/JWKS with zero help from this library). What
`security-platform-lib` adds is a _cleaner, more observable, more composable integration
surface_ over that existing machinery - registrable/orderable middleware, a single event model
for every failure mode, and a context object application code can use without knowing Spring
Security's internal types. That is the whole value proposition; it is explicitly not trying to
be "a second Spring Security."

## Concept glossary (deliberately kept distinct)

| Concept                | Who's responsible                                                                                                                                                                  | Where in this repo                                          |
| ---------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ----------------------------------------------------------- |
| **Authentication**     | Keycloak issues the identity assertion (the JWT); `security-platform-lib`'s `AuthenticationMiddleware` + `TokenAuthenticator` verify and adopt it into `SecurityContextHolder`     | `security.auth`, `security.pipeline`                        |
| **Authorization**      | `AuthorizationPolicy` (application-provided), evaluated by the built-in `AuthorizationMiddleware`                                                                                  | `security.authorization`                                    |
| **Token acquisition**  | Keycloak's token endpoint; for service-to-service, Spring Security's `OAuth2AuthorizedClientManager` (wrapped by `ClientCredentialsAuthMiddleware`) calls it and caches the result | `spring-api-example`'s `OAuth2ClientConfig`/`http` packages |
| **Token validation**   | Spring Security's `JwtDecoder` (Nimbus-backed), configured by `security-platform-lib`'s auto-configuration from `security-platform.jwt.*`                                          | `security.autoconfigure`                                    |
| **Request forwarding** | `http-client-lib`'s `ForwardingPolicy`/`ForwardingDecision`/`ForwardingExecutor` - a _business/policy_ decision about whether to call downstream at all                            | `http.forwarding`                                           |
| **HTTP transport**     | The internal `HttpEngine`/`WebClientHttpEngine` adapter - deliberately hidden behind `http-client-lib`'s own request/response model                                                | `http.spring`, `http.internal`                              |

## Request lifecycle stages actually emitted (from the library, not re-invented for this demo)

`security-platform-lib`'s `SecurityLifecyclePhase` (context state) and `SecurityEventType`
(published events) are the _only_ source of stage names anywhere in this environment -
`AuditSecurityEventConsumer` (spring-api-example) and `DownstreamSecurityEventConsumer`
(downstream-api) both just subscribe to the real `SecurityEventDrill` and log whatever it
publishes:

```
RECEIVED
  -> AUTHENTICATING            (event: AUTHENTICATION_STARTED)
  -> AUTHENTICATED             (event: AUTHENTICATION_SUCCEEDED)
     | (or AUTHENTICATION_FAILED / INVALID_TOKEN / TOKEN_EXPIRED / MISSING_CREDENTIALS)
  -> AUTHORIZING               (event: AUTHORIZATION_STARTED)
  -> AUTHORIZED                (event: AUTHORIZATION_SUCCEEDED)
     | (or AUTHORIZATION_FAILED / ACCESS_DENIED)
  -> [controller runs, http-client-lib dispatches to downstream-api]
  -> COMPLETED                 (event: PROCESSING_COMPLETED)
```

The HTTP side mirrors this independently via `http.pipeline.HttpPipeline` (stages
`dispatch`/`deserialize`, each with start/end/duration/success, visible in
`HttpResponse.trace()`) and `HttpEventDrill` (`REQUEST_STARTED`, `DOWNSTREAM_CLIENT_ERROR`,
`DOWNSTREAM_SERVER_ERROR`, `DESERIALIZATION_FAILED`, `REQUEST_COMPLETED`, ...) - see the root
README's HTTP pipeline diagrams for the exact stage set.

## Service-to-service authentication in detail

```
spring-api-example (OrderService/GatewayController)
      |
      | httpClient.execute(HttpRequestSpec.get(downstream.base-url + "/internal/inventory/{sku}"))
      v
http-client-lib's HttpMiddlewareRegistry
      |
      | "service-auth" middleware (ClientCredentialsAuthMiddleware) runs first:
      |   OAuth2AuthorizedClientManager.authorize(withClientRegistrationId("downstream-service"))
      |     -> (cache miss) POST to Keycloak's token endpoint, grant_type=client_credentials,
      |        client_id=service-client, client_secret=<dev-only>
      |     -> Keycloak validates the client secret + returns an access token whose subject is
      |        the service account "service-account-service-client", roles=[DOWNSTREAM_ACCESS]
      |   request.withHeader("Authorization", "Bearer <token>")
      v
downstream-api's SecurityMiddlewareFilter (the SAME security-platform-lib, reused independently)
      |
      | AuthenticationMiddleware validates the token against the SAME Keycloak JWKS
      | AuthorizationMiddleware requires ROLE_DOWNSTREAM_ACCESS - present, since the
      |   service account has that realm role
      v
InventoryController / OrdersController / UsersController respond
```

`http-client-lib` never imports anything from Spring Security's OAuth2 client packages or
Keycloak - `ClientCredentialsAuthMiddleware` lives entirely in `spring-api-example` and is just
another `HttpMiddleware` as far as the library is concerned. This is the "clean abstraction"
requirement: swapping Keycloak for a different IdP would mean changing
`ClientCredentialsAuthMiddleware`'s configuration, not touching `http-client-lib` at all.

## Why downstream-api reuses security-platform-lib too

`downstream-api` depends on `common-lib` exactly like `spring-api-example` - same
`SecurityMiddlewareFilter`, same `AuthorizationMiddleware`, same event model, configured against
the same Keycloak. This is the strongest proof point available that the library is a genuinely
reusable _platform_ component rather than something wired specifically for one application.
