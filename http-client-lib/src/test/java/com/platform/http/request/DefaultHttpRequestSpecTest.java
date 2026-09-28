package com.platform.http.request;

import com.platform.http.api.HttpMethodType;
import com.platform.http.api.HttpRequestSpec;
import com.platform.http.api.RequestBody;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultHttpRequestSpecTest {

    @Test
    void getAndPostFactoriesSetMethodAndUri() {
        assertThat(HttpRequestSpec.get("/users").build().method()).isEqualTo(HttpMethodType.GET);
        assertThat(HttpRequestSpec.post("/orders").build().method()).isEqualTo(HttpMethodType.POST);
    }

    @Test
    void queryParamsAreEncodedIntoResolvedUri() {
        HttpRequestSpec request = HttpRequestSpec.get("/search")
                .queryParam("q", "hello world")
                .queryParam("page", "2")
                .build();

        assertThat(request.resolvedUri()).isEqualTo("/search?q=hello+world&page=2");
    }

    @Test
    void headersAreCollectedAndBodyDefaultsToEmpty() {
        HttpRequestSpec request = HttpRequestSpec.post("/orders").header("X-Tenant", "acme").build();

        assertThat(request.headers()).containsEntry("X-Tenant", java.util.List.of("acme"));
        assertThat(request.body()).isInstanceOf(RequestBody.Empty.class);
    }

    @Test
    void bodyProducesJsonRequestBody() {
        record Order(String id) {
        }
        HttpRequestSpec request = HttpRequestSpec.post("/orders").body(new Order("42")).build();

        assertThat(request.body()).isInstanceOfSatisfying(
                RequestBody.Json.class, json -> assertThat(json.value()).isEqualTo(new Order("42")));
    }

    @Test
    void idempotentDefaultsBasedOnMethodUnlessOverridden() {
        assertThat(HttpRequestSpec.get("/x").build().idempotent()).isTrue();
        assertThat(HttpRequestSpec.post("/x").build().idempotent()).isFalse();
        assertThat(HttpRequestSpec.post("/x").idempotent(true).build().idempotent()).isTrue();
    }

    @Test
    void withHeaderAndWithCorrelationIdReturnNewImmutableInstances() {
        HttpRequestSpec original = HttpRequestSpec.get("/x").build();
        HttpRequestSpec updated = original.withHeader("X-New", "value").withCorrelationId("abc-123");

        assertThat(original.headers()).doesNotContainKey("X-New");
        assertThat(updated.headers()).containsEntry("X-New", java.util.List.of("value"));
        assertThat(updated.correlationId()).isEqualTo("abc-123");
    }

    @Test
    void defaultTimeoutIsApplied() {
        assertThat(HttpRequestSpec.get("/x").build().timeout()).isEqualTo(Duration.ofSeconds(10));
        assertThat(HttpRequestSpec.get("/x").timeout(Duration.ofSeconds(1)).build().timeout())
                .isEqualTo(Duration.ofSeconds(1));
    }
}
