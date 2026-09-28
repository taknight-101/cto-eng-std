package com.platform.http.internal;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.http.api.HttpRequestSpec;
import com.platform.http.api.HttpResponse;
import com.platform.http.api.ResponseDeserializationException;
import com.platform.http.events.DefaultHttpEventDrill;
import com.platform.http.events.HttpEventType;
import com.platform.http.middleware.DefaultHttpMiddlewareRegistry;
import com.platform.http.middleware.HttpMiddlewareOutcome;
import com.platform.http.middleware.HttpMiddlewareRegistry;
import com.platform.http.pipeline.RequestExecutionTrace;
import com.platform.http.response.DefaultHttpResponse;
import com.platform.http.spi.RetryPolicy;
import com.platform.http.spring.WebClientHttpEngine;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DefaultHttpClientTest {

    private MockWebServer server;
    private HttpMiddlewareRegistry registry;
    private DefaultHttpEventDrill eventDrill;

    record User(String id, String name) {
    }

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
        registry = new DefaultHttpMiddlewareRegistry();
        eventDrill = new DefaultHttpEventDrill();
    }

    @AfterEach
    void tearDown() throws IOException {
        server.shutdown();
    }

    private DefaultHttpClient client(RetryPolicy retryPolicy) {
        WebClient webClient = WebClient.builder().build();
        ObjectMapper mapper = new ObjectMapper();
        WebClientHttpEngine engine = new WebClientHttpEngine(webClient, new JacksonBodySerializer(mapper));
        return new DefaultHttpClient(engine, registry, eventDrill, new JacksonBodyDeserializer(mapper), retryPolicy);
    }

    private String url(String path) {
        return server.url(path).toString();
    }

    @Test
    void executesSuccessfulTypedRequest() {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .setBody("{\"id\":\"1\",\"name\":\"alice\"}"));

        DefaultHttpClient client = client(RetryPolicy.none());
        HttpResponse<User> response = client.execute(HttpRequestSpec.get(url("/users/1")).build(), User.class);

        assertThat(response.status()).isEqualTo(200);
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.body()).isEqualTo(new User("1", "alice"));
    }

    @Test
    void classifiesDownstreamServerErrorAndPublishesEvent() {
        server.enqueue(new MockResponse().setResponseCode(500).setBody("boom"));
        AtomicInteger serverErrorEvents = new AtomicInteger();
        eventDrill.subscribe(Set.of(HttpEventType.DOWNSTREAM_SERVER_ERROR), e -> serverErrorEvents.incrementAndGet());

        DefaultHttpClient client = client(RetryPolicy.none());
        HttpResponse<String> response = client.execute(HttpRequestSpec.get(url("/flaky")).build(), String.class);

        assertThat(response.isServerError()).isTrue();
        assertThat(serverErrorEvents.get()).isEqualTo(1);
    }

    @Test
    void retriesIdempotentRequestOnRetryableStatus() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(503));
        server.enqueue(new MockResponse().setResponseCode(503));
        server.enqueue(new MockResponse().setResponseCode(200).addHeader("Content-Type", "text/plain").setBody("ok"));

        RetryPolicy retryPolicy = new RetryPolicy(3, Duration.ofMillis(1), 1.0, Duration.ofMillis(5), Set.of(503), true);
        DefaultHttpClient client = client(retryPolicy);
        HttpResponse<String> response = client.execute(HttpRequestSpec.get(url("/retry")).build(), String.class);

        assertThat(response.status()).isEqualTo(200);
        assertThat(server.getRequestCount()).isEqualTo(3);
    }

    @Test
    void doesNotRetryNonIdempotentRequestByDefault() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(503));

        RetryPolicy retryPolicy = new RetryPolicy(3, Duration.ofMillis(1), 1.0, Duration.ofMillis(5), Set.of(503), true);
        DefaultHttpClient client = client(retryPolicy);
        HttpResponse<String> response = client.execute(HttpRequestSpec.post(url("/orders")).body(Map.of()).build(), String.class);

        assertThat(response.status()).isEqualTo(503);
        assertThat(server.getRequestCount()).isEqualTo(1);
    }

    @Test
    void deserializationFailurePublishesEventAndThrows() {
        server.enqueue(new MockResponse().setResponseCode(200).addHeader("Content-Type", "application/json")
                .setBody("not-json"));
        AtomicInteger deserializationFailures = new AtomicInteger();
        eventDrill.subscribe(Set.of(HttpEventType.DESERIALIZATION_FAILED), e -> deserializationFailures.incrementAndGet());

        DefaultHttpClient client = client(RetryPolicy.none());
        assertThatThrownBy(() -> client.execute(HttpRequestSpec.get(url("/bad")).build(), User.class))
                .isInstanceOf(ResponseDeserializationException.class);
        assertThat(deserializationFailures.get()).isEqualTo(1);
    }

    @Test
    void middlewareCanShortCircuitBeforeDownstreamIsCalled() {
        Instant now = Instant.now();
        registry.register("cache", (req, chain) -> HttpMiddlewareOutcome.respond(
                new DefaultHttpResponse<>(200, Map.of(), "{\"id\":\"cached\",\"name\":\"n\"}".getBytes(), "{\"id\":\"cached\",\"name\":\"n\"}".getBytes(),
                        req, now, now, RequestExecutionTrace.empty(req.correlationId()))));

        DefaultHttpClient client = client(RetryPolicy.none());
        HttpResponse<User> response = client.execute(HttpRequestSpec.get(url("/users/1")).build(), User.class);

        assertThat(response.body()).isEqualTo(new User("cached", "n"));
        assertThat(server.getRequestCount()).isZero();
    }

    @Test
    void eventConsumerFailureIsIsolatedFromOtherConsumersAndClient() {
        server.enqueue(new MockResponse().setResponseCode(200).addHeader("Content-Type", "text/plain").setBody("ok"));
        AtomicInteger goodConsumerCalls = new AtomicInteger();
        eventDrill.subscribe(event -> {
            throw new RuntimeException("misbehaving consumer");
        });
        eventDrill.subscribe(event -> goodConsumerCalls.incrementAndGet());

        DefaultHttpClient client = client(RetryPolicy.none());
        HttpResponse<String> response = client.execute(HttpRequestSpec.get(url("/ok")).build(), String.class);

        assertThat(response.status()).isEqualTo(200);
        assertThat(goodConsumerCalls.get()).isGreaterThan(0);
    }

    @Test
    void responseCarriesExecutionTraceWithDispatchAndDeserializeStages() {
        server.enqueue(new MockResponse().setResponseCode(200).addHeader("Content-Type", "text/plain").setBody("ok"));

        DefaultHttpClient client = client(RetryPolicy.none());
        HttpResponse<String> response = client.execute(HttpRequestSpec.get(url("/traced")).build(), String.class);

        List<String> stageNames = response.trace().stages().stream().map(s -> s.name()).toList();
        assertThat(stageNames).containsExactly("dispatch", "deserialize");
    }
}
