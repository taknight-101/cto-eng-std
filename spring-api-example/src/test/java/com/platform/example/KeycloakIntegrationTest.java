package com.platform.example;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.MountableFile;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves security-platform-lib's JWT validation against a REAL Keycloak - not a fake issuer -
 * running in a Testcontainers-managed container, complementing {@link ExampleApplicationTests}
 * (which uses the fast, self-contained HMAC demo path).
 *
 * <p>Unlike the docker-compose environment, no fixed {@code KC_HOSTNAME} trick is needed here:
 * the test JVM and the Spring context under test are the same process, so both reach Keycloak
 * via the same Testcontainers-assigned host/port, and the issuer is naturally self-consistent.
 * Uses Keycloak's dev-mode (in-memory) storage - no Postgres container needed for this test.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class KeycloakIntegrationTest {

    private static final String REALM_JSON_CONTAINER_PATH = "/opt/keycloak/data/import/common-platform-realm.json";

    @Container
    static final GenericContainer<?> keycloak = new GenericContainer<>("quay.io/keycloak/keycloak:26.0")
            .withExposedPorts(8080)
            .withEnv("KEYCLOAK_ADMIN", "admin")
            .withEnv("KEYCLOAK_ADMIN_PASSWORD", "admin-dev-only-password")
            .withCopyFileToContainer(
                    MountableFile.forHostPath("../infra/keycloak/realm/common-platform-realm.json"),
                    REALM_JSON_CONTAINER_PATH)
            .withCommand("start-dev", "--import-realm")
            .waitingFor(Wait.forHttp("/realms/common-platform/.well-known/openid-configuration")
                    .forStatusCode(200)
                    .withStartupTimeout(Duration.ofMinutes(3)));

    @DynamicPropertySource
    static void keycloakProperties(DynamicPropertyRegistry registry) {
        String base = String.format("http://%s:%d", keycloak.getHost(), keycloak.getMappedPort(8080));
        registry.add("security-platform.jwt.enabled", () -> "true");
        registry.add("security-platform.jwt.hmac-secret", () -> "");
        registry.add("security-platform.jwt.jwk-set-uri", () -> base + "/realms/common-platform/protocol/openid-connect/certs");
        registry.add("security-platform.jwt.issuer", () -> base + "/realms/common-platform");
        registry.add("security-platform.jwt.audience", () -> "common-platform-api");
    }

    @LocalServerPort
    private int port;

    private final TestRestTemplate restTemplate = new TestRestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private String baseUrl() {
        return "http://localhost:" + port;
    }

    private String tokenFor(String username, String password) {
        String tokenUrl = String.format("http://%s:%d/realms/common-platform/protocol/openid-connect/token",
                keycloak.getHost(), keycloak.getMappedPort(8080));
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        String body = "grant_type=password&client_id=interactive-client&username=" + username + "&password=" + password;
        ResponseEntity<String> response = restTemplate.postForEntity(tokenUrl, new HttpEntity<>(body, headers), String.class);
        try {
            JsonNode node = objectMapper.readTree(response.getBody());
            return node.get("access_token").asText();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse token response: " + response.getBody(), e);
        }
    }

    @Test
    void realKeycloakIssuedTokenAuthenticatesSuccessfully() {
        String token = tokenFor("alice", "password");

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl() + "/api/users/1", HttpMethod.GET, new HttpEntity<>(headers), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("requestedBy", "alice");
    }

    @Test
    void requestWithoutTokenIsRejected() {
        ResponseEntity<String> response = restTemplate.getForEntity(baseUrl() + "/api/users/1", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void userWithoutAdminRoleCannotPlaceOrders() {
        String token = tokenFor("alice", "password");
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<String> response = restTemplate.postForEntity(
                baseUrl() + "/api/orders", new HttpEntity<>("{\"sku\":\"WIDGET\",\"quantity\":1}", headers), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void adminCanPlaceOrders() {
        String token = tokenFor("admin", "password");
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<Map> response = restTemplate.exchange(baseUrl() + "/api/orders", HttpMethod.POST,
                new HttpEntity<>("{\"sku\":\"WIDGET\",\"quantity\":1}", headers), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).containsEntry("status", "CONFIRMED");
    }
}
