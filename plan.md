# Plan: Common Platform — Security + HTTP Client Java Libraries

## Confirmed decisions

- Full-depth implementation (all abstractions, full tests, full example, docs/javadoc).
- Servlet-only (Spring MVC + classic Spring Security filter chain). Reactive/WebFlux explicitly out of scope, documented.
- HTTP client public API: sync facade (`HttpResponse<T>` blocking return) + async escape hatch (`Mono<HttpResponse<T>>` via `executeReactive`).
- Underlying HTTP engine: Spring WebClient, hidden behind internal `HttpEngine` SPI (`http.spring` package).
- JWT: Nimbus JOSE+JWT for signature verification/claims parsing (aligns with Spring Security OAuth2 resource server internals).
- Java 21 + Spring Boot 3.3.x + Spring Framework 6.1.x.
- common-lib = aggregator dependency (compile-scope deps on both libs), NOT a shaded uber-jar. Tradeoff documented in README.
- HTTP integration tests: OkHttp MockWebServer.
- No shared pipeline/event/middleware abstraction between the two libraries (deliberate duplication, no vague `CommonPipeline<T>`).

## Maven coordinates / layout

- Build directly at workspace root `c:\Users\robot\Desktop\CTO` (no extra nesting folder).
- groupId: `com.platform`. version `1.0.0-SNAPSHOT`.
- Root `pom.xml` = parent + aggregator (packaging=pom), modules: security-platform-lib, http-client-lib, common-lib, spring-api-example.
- Root pom: dependencyManagement imports `spring-boot-dependencies` BOM, java.version=21, shared plugin management. No separate BOM module.
- common-lib: thin module, POM-only deps on security-platform-lib + http-client-lib (compile scope) + a `com.platform.common` package-info.java. No facade class — both libs' AutoConfiguration.imports activate automatically once both jars are on classpath.

## Package structure

### security-platform-lib (`com.platform.security`)

- `security.api` — public interfaces + exceptions: `SecurityRequestContext`, `SecurityMiddleware`, `SecurityMiddlewareChain`, `SecurityMiddlewareRegistry`, `SecurityPipeline`, `TokenAuthenticator`, `JwtAuthenticator`, `OAuth2Authenticator`, `SecurityContextAccessor`, `AuthorizationPolicy`, `AuthorizationDecision`, `SecurityEvent`, `SecurityEventType`, `SecurityEventConsumer`, `SecurityEventDrill`, `PlatformException`/`SecurityPlatformException` + subclasses.
- `security.context` — `DefaultSecurityRequestContext` impl, `SecurityLifecyclePhase` enum, immutable snapshot/copy-on-transition model.
- `security.pipeline` — `DefaultSecurityMiddlewareRegistry`, `DefaultSecurityPipeline` (compiles chain from registry w/ version counter cache).
- `security.auth` — `BearerTokenExtractor`, `NimbusJwtAuthenticator`, `JwtValidator` SPI + `NimbusJwtValidator`, `JwtClaims` value object, `AuthenticationProviderAdapter`, `OAuth2Authenticator` impl delegating to Spring resource server.
- `security.authorization` — `AuthorizationPolicies` factory: requireAuthority/requireAnyAuthority/allowIf/and/or/negate.
- `security.events` — `DefaultSecurityEventDrill` (try/catch isolation per consumer), `LoggingSecurityEventConsumer` w/ redaction.
- `security.spi` — extension points for user implementation: `TokenValidator`, `SecurityContextEnricher`, `FailureHandler`, `RequestInspector`.
- `security.spring` — `SecurityMiddlewareFilter` (`OncePerRequestFilter`), `SpringSecurityContextAdapter`, `SecurityPlatformConfigurer` (HttpSecurity DSL entry), bridges to `AuthenticationEntryPoint`/`AccessDeniedHandler`.
- `security.autoconfigure` — `SecurityPlatformAutoConfiguration`, `SecurityPlatformProperties` (`security-platform`), `AutoConfiguration.imports`.
- `security.internal` — redaction utils, correlation id generator, glue over `SecurityContextHolder`.

### http-client-lib (`com.platform.http`)

- `http.api` — `HttpRequestSpec` + Builder, `HttpResponse<T>`, `HttpClient`, `TypeRef<T>`, `DispatchResult<T>` outcome type, `PlatformException`/`HttpClientPlatformException` + subclasses.
- `http.request` — `DefaultHttpRequestSpec`/builder impl, `RequestBody` sealed variants.
- `http.response` — `DefaultHttpResponse` impl, timing metadata.
- `http.pipeline` — `HttpPipeline` stage executor, `RequestExecutionTrace`, `StageRecord`.
- `http.dispatch` — `DefaultDispatcher` (`Dispatcher<R,T>` impl using `DispatchResult` chain: map/flatMap/onSuccess/onFailure/consume).
- `http.forwarding` — `ForwardingPolicy` SPI, `ForwardingDecision` sealed: Forward/ShortCircuit/Reject/Retry/Transform, `ForwardingExecutor` w/ retry-loop guard.
- `http.middleware` — `HttpMiddleware`, `HttpMiddlewareChain`, `HttpMiddlewareRegistry` (independent impl mirroring security pipeline shape, no shared base).
- `http.events` — `HttpEventDrill`, `HttpEvent`, `LoggingHttpEventConsumer`.
- `http.spi` — `RetryPolicy` config type, Serializer/Deserializer SPI.
- `http.spring` — internal `HttpEngine` interface, `WebClientHttpEngine` impl (adapter boundary keeping WebClient out of public API).
- `http.autoconfigure` — `HttpClientAutoConfiguration`, `HttpClientProperties` (`http-client`), `AutoConfiguration.imports`.
- `http.internal` — `DefaultHttpClient` (sync facade over engine + `.block()`, `executeReactive`), retry executor, redaction.

## Key architectural decisions to preserve

- `SecurityMiddleware`: single composable method `handle(context, chain)` returning outcome + default `onError` method.
- `SecurityRequestContext` is immutable; each lifecycle transition produces a new instance; mutable orchestration confined to internal pipeline executor.
- `SecurityContextAccessor` delegates storage to Spring's own `SecurityContextHolder` (not a parallel ThreadLocal); provides safe `runAs(auth, action)` scoped execution restoring prior context in finally.
- Event drills (security + http) isolate consumer exceptions via try/catch per consumer; never rethrow into primary pipeline; never re-publish consumer failures as new events.
- Dispatch API uses custom `DispatchResult<T>` (Success/Failure) rather than `Mono` for the public declarative dispatch chain; `Mono` remains available only via `executeReactive` escape hatch.
- `RetryPolicy` defaults to idempotent-only retries — never silently retries POST/PATCH.
- Exception hierarchy kept lean (~5-6 subclasses per library) using error codes for finer distinction.
- No shared generic pipeline/event abstraction between the two libraries.

## Phases / Steps (B and E are parallel-buildable)

- [x] A. Scaffolding: root pom.xml + 4 module pom.xml files, directory skeletons, plugin/dependency management.
- [x] B. security-platform-lib core: api, context, pipeline, authorization, events, spi packages + unit tests.
- [x] C. security-platform-lib Spring integration: security.spring (filter, configurer, adapters), security.auth (JWT via Nimbus, OAuth2 adapter). Depends on B.
- [x] D. security-platform-lib autoconfigure + properties. Depends on C.
- [x] E. http-client-lib core: api, request, response, dispatch, forwarding, middleware, pipeline, events, spi packages (parallel with B/C/D).
- [x] F. http-client-lib Spring integration: http.spring (WebClientHttpEngine), http.internal (DefaultHttpClient). Depends on E.
- [x] G. http-client-lib autoconfigure + properties. Depends on F.
- [x] H. Unit + integration tests for both libraries. Depends on B–G. (38 security tests + 33 http tests, all passing)
- [x] I. common-lib module (thin aggregator). Depends on D, G.
- [x] J. spring-api-example: GET /api/users/{id}, POST /api/orders, demo JWT, audit middleware, authorization rules, event consumer beans, declarative HTTP client call, conditional forwarding (GatewayController), pipeline trace logging. Depends on I. (6 end-to-end tests passing)
- [x] K. README.md + JavaDoc pass on all public types (package-info.java already added for every package). Depends on J.
- [x] L. Final architectural review pass: cross-library coupling, Spring-type leakage, threading/ThreadLocal cleanup, blocking-call audit, filter ordering, event recursion, swallowed exceptions, retry idempotency gate, redaction coverage. Depends on K.

`mvn clean verify` passes end-to-end: 77 tests total (38 security-platform-lib + 33 http-client-lib + 6 spring-api-example).

### Final review findings (all clean)

- Zero cross-imports between `com.platform.security` and `com.platform.http` packages.
- No Spring/WebClient imports in `http.api`; only documented `Authentication` imports in `security.api`
  (AuthenticationOutcome, SecurityContextAccessor - deliberate, not leakage).
- Single `.block(` call site, isolated in `WebClientHttpEngine`.
- No empty catch blocks in main sources.
- `SecurityContextHolder.clearContext()` runs in `SecurityMiddlewareFilter`'s `finally`.
- Event drills verified via tests to isolate consumer failures without re-publishing/recursion.
- Retry gated by `RetryPolicy.idempotentOnly` + `HttpRequestSpec.idempotent()` (verified by test).

## Status: COMPLETE. All 12 phases done, full build green.

NOTE: Full reactor (`mvn -q compile`) compiles cleanly as of end of phase G. Both AutoConfiguration.imports files in place.
Key design realized during implementation: HTTP middleware terminal step performs the actual retry-aware
dispatch and wraps it as a ShortCircuit outcome, so middleware naturally gets full "around" (before+after)
semantics via a single `handle(request, chain)` method - matches the security middleware model.

## Verification

- `mvn -q clean verify` from workspace root must succeed (compiles all modules, runs unit+integration tests via surefire/failsafe).
- Run spring-api-example (`mvn -pl spring-api-example spring-boot:run`), curl GET /api/users/1 with/without valid demo JWT (401 vs 200), curl POST /api/orders, inspect logs for pipeline trace + event consumer output, confirm no raw token in logs.
- Grep public API packages (`*.api`, excluding `*.spring`/`*.internal`) for forbidden imports (`org.springframework.security.core.Authentication`, `org.springframework.web.reactive.function.client.WebClient`) to confirm no framework leakage.
