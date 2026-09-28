package com.platform.downstream;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

/** Fast, no-external-infra test: proves the resource-server pipeline rejects unauthenticated
 * requests without needing a live Keycloak. Full JWT round-trip is covered by the
 * Testcontainers-based integration test at the repository root. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class DownstreamApiApplicationTests {

    @LocalServerPort
    private int port;

    private final TestRestTemplate restTemplate = new TestRestTemplate();

    @Test
    void requestWithoutTokenIsRejected() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/internal/users/1", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
