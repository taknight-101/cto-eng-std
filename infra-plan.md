# Infra Plan: Real Keycloak/OAuth2/Downstream-API Integration Environment

## Goal

Add a local, reproducible infrastructure environment (Keycloak + Postgres + a real
`downstream-api` service + scripts) so `security-platform-lib` and `http-client-lib` can be
exercised against real OAuth2/OIDC infrastructure, not fakes - without breaking the existing
self-contained (HMAC demo JWT, in-process inventory) fast unit-test path.

## Key architecture decisions

1. **Two run modes for `spring-api-example`, not a replacement:**
   - Default profile (unchanged): HMAC demo JWT (`DevTokenController`), in-process
     `InventoryController`. Zero external dependencies; `mvn test` / bare `mvnw spring-boot:run`
     keep working exactly as before.
   - New `docker` profile: real Keycloak JWKS validation, real separate `downstream-api`
     service, client-credentials OAuth2 middleware for service-to-service calls.
   - `DevTokenController` gets `@Profile("!docker")` so it doesn't mislead users once real
     Keycloak validation is active.

2. **Keycloak issuer consistency (the classic Docker+OIDC trap):** set `KC_HOSTNAME=keycloak`
   (fixed), so the `iss` claim and discovery document are **always**
   `http://keycloak:8080/realms/common-platform`, regardless of whether Keycloak was reached via
   `localhost:8080` (host/browser/curl) or `keycloak:8080` (container-to-container). Both
   `spring-api-example` and `downstream-api` (in containers) use that same issuer-uri and resolve
   it via Docker's embedded DNS - no `host.docker.internal` tricks needed.
   - Running an app on the bare host (`mvnw spring-boot:run`) against the same issuer requires a
     one-line hosts-file entry (`127.0.0.1 keycloak`), documented in `infra/README.md`.

3. **No shaded/second implementation, no nginx.** No TLS/nginx layer is added (documented as
   deliberately out of scope - HTTP is sufficient for a local demo; adding TLS+nginx would add
   container count without proving anything new about the libraries). Observability
   (Jaeger + OTel Collector) is added behind an optional Compose profile using the OTel Java
   agent (zero app-code changes) rather than hand-wiring OTel SDK.

4. **Realm role flattening:** Keycloak's default `realm_access.roles` claim is nested; our
   `JwtAuthorityMappers.fromClaim("roles", "ROLE_")` expects a flat top-level claim. Add a
   Keycloak protocol mapper (`usermodel.realmRoleMapping`, token claim name `roles`,
   multivalued) via a shared client scope, on both the interactive and service clients.

5. **Audience validation gap fix:** `SecurityPlatformProperties.Jwt.audience` already exists but
   was never wired into an actual validator. Fix `SecurityPlatformAutoConfiguration.jwtDecoder()`
   to compose a real audience check (via `DelegatingOAuth2TokenValidator`) when configured, and
   set `aud=common-platform-api` in the realm so this is actually exercised end-to-end.

6. **Service-to-service call stays library-agnostic about Keycloak.** A new
   `ClientCredentialsAuthMiddleware` (`HttpMiddleware`, lives in `spring-api-example`, not in
   `http-client-lib`) uses Spring's `OAuth2AuthorizedClientManager` to obtain/cache a
   client-credentials token and adds `Authorization: Bearer ...`. `http-client-lib` itself never
   learns about Keycloak/OAuth2 - it only sees a middleware that mutates headers.

7. **Correlation ID propagation:** add `com.platform.security.spring.CurrentSecurityContext`
   (reads the request-scoped `SecurityRequestContext` already stored by
   `SecurityMiddlewareFilter`) so `OrderService`/`GatewayController` can propagate the _same_
   `X-Correlation-Id` from the inbound security context into the outbound `HttpRequestSpec`
   instead of generating a new, unrelated one.

8. **Testcontainers over pure Compose-dependent tests for CI.** A new integration test spins up
   Keycloak (generic container, realm imported at startup) + Postgres via Testcontainers,
   overriding `security-platform.jwt.*` via `@DynamicPropertySource` - no fixed-hostname trick
   needed since Testcontainers gives a real, directly-reachable mapped port in-process.

## New/changed components

| Component                     | Change                                                                                                                                                                                                                                                                                                                                                                                    |
| ----------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `security-platform-lib`       | Fix audience validator wiring (real bug fix, small). Add `CurrentSecurityContext` helper.                                                                                                                                                                                                                                                                                                 |
| `downstream-api` (NEW module) | Spring Boot resource server on `common-lib`, port 8082. Endpoints: `GET /internal/users/{id}`, `GET /internal/orders/{id}`, `POST /internal/orders`, `GET /internal/inventory/{sku}`. Requires `ROLE_DOWNSTREAM_ACCESS`. One `application.yml` works local+docker (fixed Keycloak issuer).                                                                                                |
| `spring-api-example`          | Add `application-docker.yml`. Add `spring-security-oauth2-client` dependency + `OAuth2ClientConfig` (`@Profile("docker")`) with `ClientCredentialsAuthMiddleware`. Add `@Profile("!docker")` to `DevTokenController`. Propagate correlation ID via `CurrentSecurityContext`.                                                                                                              |
| `infra/` (NEW)                | `docker-compose.yml` (postgres, keycloak, downstream-api, spring-api-example, optional `observability` profile: otel-collector + jaeger), `.env.example`, `keycloak/realm/common-platform-realm.json` (realm, 3 clients, roles, users, groups, protocol mappers, all auto-imported via `--import-realm`), `scripts/{up,down,reset,health,smoke-test}.sh`, `README.md`, `ARCHITECTURE.md`. |
| Root `README.md`              | New short section linking to `infra/README.md`.                                                                                                                                                                                                                                                                                                                                           |
| Integration tests             | New Testcontainers-based test (Keycloak + Postgres) proving real JWT validation end-to-end, alongside existing fast unit tests (untouched).                                                                                                                                                                                                                                               |

## Keycloak realm design

- Realm: `common-platform`.
- Roles: `USER`, `ADMIN`, `DOWNSTREAM_ACCESS`.
- Users (password = `password`, documented as local-dev-only):
  - `alice` -> `USER`
  - `bob` -> `USER`, `DOWNSTREAM_ACCESS`
  - `admin` -> `USER`, `ADMIN`, `DOWNSTREAM_ACCESS`
- Clients:
  - `interactive-client` (public, standard flow + PKCE, direct-access-grants enabled so the
    automated smoke test can use the ROPC/password grant against `alice`/`bob`/`admin` without a
    browser; browser-based auth-code flow documented separately as a manual walkthrough).
  - `service-client` (confidential, service accounts enabled, `client_credentials`; service
    account user granted `DOWNSTREAM_ACCESS`). Used by `spring-api-example`'s
    `ClientCredentialsAuthMiddleware` to call `downstream-api`.
  - No dedicated Keycloak client for the resource servers themselves (not required for pure
    bearer-token validation) - documented explicitly as an intentional simplification.
- Client scope `platform-roles`: flattens `realm_access.roles` into a top-level `roles` claim.
- Client scope `platform-audience`: sets `aud=common-platform-api`.

## Implementation order

1. `security-platform-lib`: audience validator fix + `CurrentSecurityContext` helper. Compile/test.
2. `downstream-api` new module (pom + app + security wiring + endpoints). Add to root `pom.xml` modules. Compile/test.
3. `spring-api-example`: `application-docker.yml`, OAuth2 client config + middleware, correlation-id propagation, `DevTokenController` profile guard. Compile/test (existing tests must stay green).
4. `infra/`: realm JSON, docker-compose.yml, .env.example, scripts, README.md, ARCHITECTURE.md.
5. Testcontainers integration test.
6. Root README update (short pointer section).
7. Full `mvn clean verify` + manual `docker compose up` smoke pass (to the extent it can be run in this environment).

## Status

Implementation complete:

- `security-platform-lib`: audience validator wired for real (was previously a dead property), `CurrentSecurityContext` helper added.
- `downstream-api` new module: resource server on `common-lib`, 4 endpoints, `ROLE_DOWNSTREAM_ACCESS` policy, actuator health, own security-event consumer. Compiles + unit test passes.
- `spring-api-example`: `application-docker.yml`, `OAuth2ClientConfig` + `ClientCredentialsAuthMiddleware` (profile `docker` only), correlation-ID propagation via `CurrentSecurityContext`, `DevTokenController` guarded with `@Profile("!docker")`, actuator health added.
- `infra/`: realm JSON (validated, 3 roles, 2 clients incl. service account role mapping, 3 users, role-flattening + audience protocol mappers), `docker-compose.yml` (validated via `docker compose config`, including the `observability` profile), `.env.example`, Dockerfiles for both apps (multi-stage, whole-reactor build context), OTel collector config, 5 scripts (`up/down/reset/health/smoke-test.sh`, all executable + `bash -n` syntax-checked), `README.md`, `ARCHITECTURE.md`.
- `KeycloakIntegrationTest` (Testcontainers, real Keycloak, no fixed-hostname trick needed since test JVM and app share the same network view): compiles; excluded from default `mvn test`/`verify` via Surefire exclude + `docker-tests` Maven profile to re-include it, since no Docker daemon is available in this sandbox to actually run it end-to-end. **User should run `mvn -pl spring-api-example -am test -Pdocker-tests` and `docker compose -f infra/docker-compose.yml up` themselves to verify the full container build/run**, since builds requiring a live Docker daemon could not be executed here (`docker compose build` failed with "the system cannot find the file specified" - Docker Desktop installed but not running).
- Root README.md: new pointer section + updated table of contents.
- `.gitignore` added (ignores `target/`, `infra/.env`).
- Full `mvn clean verify`: 84 tests, BUILD SUCCESS.

Not independently verified here (no running Docker daemon in this environment): `docker compose build/up`, `scripts/health.sh`, `scripts/smoke-test.sh` end-to-end, `KeycloakIntegrationTest` actually running. All were validated as much as possible without a daemon (`docker compose config`, `bash -n`, `python -c json.load`, Maven compile).
