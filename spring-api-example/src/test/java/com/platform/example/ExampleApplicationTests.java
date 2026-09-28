package com.platform.example;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end walkthrough of the example API: JWT authentication, authorization, declarative
 * HTTP client dispatch to the in-process "downstream" service, and conditional forwarding.
 */
@SpringBootTest(classes = ExampleApplication.class, webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
class ExampleApplicationTests {

    private final TestRestTemplate restTemplate = new TestRestTemplate();
    private static final String BASE_URL = "http://localhost:8081";

    @Test
    void requestWithoutTokenIsRejected() {
        ResponseEntity<String> response = restTemplate.getForEntity(BASE_URL + "/api/users/1", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void validTokenAllowsReadingUser() {
        String token = mintToken("alice", "USER");

        ResponseEntity<Map> response = restTemplate.exchange(
                BASE_URL + "/api/users/1", HttpMethod.GET, authorized(token), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("requestedBy", "alice");
    }

    @Test
    void placingOrderRequiresAdminRole() {
        String userToken = mintToken("bob", "USER");
        ResponseEntity<String> forbidden = restTemplate.exchange(
                BASE_URL + "/api/orders", HttpMethod.POST,
                authorized(userToken, Map.of("sku", "WIDGET", "quantity", 1)), String.class);
        assertThat(forbidden.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        String adminToken = mintToken("carol", "ADMIN");
        ResponseEntity<Map> created = restTemplate.exchange(
                BASE_URL + "/api/orders", HttpMethod.POST,
                authorized(adminToken, Map.of("sku", "WIDGET", "quantity", 1)), Map.class);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(created.getBody()).containsEntry("status", "CONFIRMED");
    }

    @Test
    void orderIsRejectedWhenDownstreamInventoryIsInsufficient() {
        String adminToken = mintToken("carol", "ADMIN");
        ResponseEntity<Map> conflict = restTemplate.exchange(
                BASE_URL + "/api/orders", HttpMethod.POST,
                authorized(adminToken, Map.of("sku", "OUT-OF-STOCK", "quantity", 1)), Map.class);
        assertThat(conflict.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void gatewayRejectsBlockedSkuWithoutCallingDownstream() {
        String token = mintToken("dave", "USER");
        ResponseEntity<Map> response = restTemplate.exchange(
                BASE_URL + "/api/gateway/inventory/BLOCKED-1", HttpMethod.GET, authorized(token), Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void gatewayForwardsAllowedSkuToDownstream() {
        String token = mintToken("dave", "USER");
        ResponseEntity<Map> response = restTemplate.exchange(
                BASE_URL + "/api/gateway/inventory/WIDGET", HttpMethod.GET, authorized(token), Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("quantity", 100);
    }

    private String mintToken(String subject, String roles) {
        ResponseEntity<Map> response = restTemplate.getForEntity(
                BASE_URL + "/dev/token?subject=" + subject + "&roles=" + roles, Map.class);
        return (String) response.getBody().get("token");
    }

    private HttpEntity<Void> authorized(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        return new HttpEntity<>(headers);
    }

    private HttpEntity<Map<String, Object>> authorized(String token, Map<String, Object> body) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }
}
