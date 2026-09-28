package com.platform.http.internal;

import com.platform.http.api.HttpClient;
import com.platform.http.api.HttpClientPlatformException;
import com.platform.http.api.HttpRequestSpec;
import com.platform.http.api.HttpResponse;
import com.platform.http.api.TypeRef;
import com.platform.http.events.HttpEvent;
import com.platform.http.events.HttpEventDrill;
import com.platform.http.events.HttpEventType;
import com.platform.http.middleware.HttpMiddlewareChain;
import com.platform.http.middleware.HttpMiddlewareException;
import com.platform.http.middleware.HttpMiddlewareOutcome;
import com.platform.http.middleware.HttpMiddlewarePipeline;
import com.platform.http.middleware.HttpMiddlewareRegistry;
import com.platform.http.pipeline.HttpPipeline;
import com.platform.http.pipeline.RequestExecutionTrace;
import com.platform.http.response.DefaultHttpResponse;
import com.platform.http.spi.BodyDeserializer;
import com.platform.http.spi.RetryPolicy;
import com.platform.http.spring.HttpEngine;
import com.platform.http.spring.RawHttpResponse;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

/**
 * Sole {@link HttpClient} implementation: runs the middleware chain (whose terminal step
 * performs the actual, retry-aware downstream dispatch), records a {@link HttpPipeline} trace,
 * deserializes the response body, and publishes {@link HttpEvent}s throughout.
 *
 * <p><strong>Blocking:</strong> {@link #execute} blocks the calling thread on the underlying
 * {@code HttpEngine} call. {@link #executeReactive} offloads that same blocking call onto a
 * bounded elastic scheduler so it does not tie up a non-blocking (e.g. Netty event-loop)
 * thread; it is a convenience bridge, not a fully non-blocking call path end-to-end.
 */
public final class DefaultHttpClient implements HttpClient {

    private final HttpEngine engine;
    private final HttpMiddlewarePipeline middlewarePipeline;
    private final HttpEventDrill eventDrill;
    private final BodyDeserializer bodyDeserializer;
    private final RetryPolicy retryPolicy;

    public DefaultHttpClient(
            HttpEngine engine,
            HttpMiddlewareRegistry registry,
            HttpEventDrill eventDrill,
            BodyDeserializer bodyDeserializer,
            RetryPolicy retryPolicy) {
        this.engine = engine;
        this.eventDrill = eventDrill;
        this.bodyDeserializer = bodyDeserializer;
        this.retryPolicy = retryPolicy;
        HttpMiddlewareChain terminal = this::terminalDispatch;
        this.middlewarePipeline = new HttpMiddlewarePipeline(registry, terminal);
    }

    @Override
    public HttpResponse<String> execute(HttpRequestSpec request) {
        return execute(request, String.class);
    }

    @Override
    public <T> HttpResponse<T> execute(HttpRequestSpec request, Class<T> responseType) {
        return doExecute(request, responseType);
    }

    @Override
    public <T> HttpResponse<T> execute(HttpRequestSpec request, TypeRef<T> responseType) {
        return doExecute(request, responseType.type());
    }

    @Override
    public <T> Mono<HttpResponse<T>> executeReactive(HttpRequestSpec request, Class<T> responseType) {
        return Mono.fromCallable(() -> execute(request, responseType)).subscribeOn(Schedulers.boundedElastic());
    }

    private <T> HttpResponse<T> doExecute(HttpRequestSpec originalRequest, Type responseType) {
        eventDrill.publish(HttpEvent.of(HttpEventType.REQUEST_STARTED, originalRequest, "start", Map.of()));
        HttpPipeline pipeline = HttpPipeline.start(originalRequest.correlationId());
        try {
            HttpMiddlewareOutcome outcome = pipeline.stage("dispatch", () -> runMiddlewareChain(originalRequest));
            if (!(outcome instanceof HttpMiddlewareOutcome.ShortCircuit shortCircuit)) {
                throw new HttpMiddlewareException(
                        "Middleware chain completed without producing a response - did a middleware forget to "
                                + "call chain.proceed()?",
                        null, "middleware-pipeline");
            }
            HttpResponse<?> rawResponse = shortCircuit.response();
            T body = pipeline.stage("deserialize", () -> bodyDeserializer.deserialize(
                    rawResponse.rawBody(), responseType, contentTypeOf(rawResponse)));
            HttpResponse<T> response = new DefaultHttpResponse<>(
                    rawResponse.status(), rawResponse.headers(), body, rawResponse.rawBody(), rawResponse.request(),
                    rawResponse.startedAt(), rawResponse.completedAt(), pipeline.trace());
            publishCompletionEvent(response);
            return response;
        } catch (HttpClientPlatformException e) {
            eventDrill.publish(mapExceptionToEvent(originalRequest, e));
            throw e;
        } catch (RuntimeException e) {
            eventDrill.publish(HttpEvent.of(HttpEventType.UNEXPECTED_ERROR, originalRequest, e, "unexpected", Map.of()));
            throw e;
        }
    }

    private HttpMiddlewareOutcome runMiddlewareChain(HttpRequestSpec request) {
        try {
            return middlewarePipeline.execute(request);
        } catch (RuntimeException re) {
            throw re;
        } catch (Exception e) {
            throw new HttpMiddlewareException("Middleware pipeline failed", e, "middleware-pipeline");
        }
    }

    private HttpMiddlewareOutcome terminalDispatch(HttpRequestSpec request) {
        RawHttpResponse raw = dispatchWithRetry(request);
        HttpResponse<byte[]> rawResponse = new DefaultHttpResponse<>(
                raw.status(), raw.headers(), raw.rawBody(), raw.rawBody(), request, raw.startedAt(),
                raw.completedAt(), RequestExecutionTrace.empty(request.correlationId()));
        return HttpMiddlewareOutcome.respond(rawResponse);
    }

    private RawHttpResponse dispatchWithRetry(HttpRequestSpec request) {
        int attempt = 1;
        while (true) {
            try {
                RawHttpResponse response = engine.execute(request);
                if (isRetryableRequest(request) && retryPolicy.isRetryable(response.status())
                        && attempt < retryPolicy.maxAttempts()) {
                    sleep(retryPolicy.backoffFor(attempt));
                    attempt++;
                    continue;
                }
                return response;
            } catch (com.platform.http.api.HttpConnectionException e) {
                if (isRetryableRequest(request) && attempt < retryPolicy.maxAttempts()) {
                    sleep(retryPolicy.backoffFor(attempt));
                    attempt++;
                    continue;
                }
                throw e;
            }
        }
    }

    private boolean isRetryableRequest(HttpRequestSpec request) {
        return !retryPolicy.idempotentOnly() || request.idempotent();
    }

    private static void sleep(java.time.Duration duration) {
        try {
            if (!duration.isZero() && !duration.isNegative()) {
                Thread.sleep(duration.toMillis());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting to retry", e);
        }
    }

    private void publishCompletionEvent(HttpResponse<?> response) {
        HttpEventType type = response.isServerError() ? HttpEventType.DOWNSTREAM_SERVER_ERROR
                : response.isClientError() ? HttpEventType.DOWNSTREAM_CLIENT_ERROR
                : HttpEventType.REQUEST_COMPLETED;
        eventDrill.publish(HttpEvent.of(type, response.request(), response, "completed", response.duration(),
                Map.of("status", response.status())));
    }

    private static HttpEvent mapExceptionToEvent(HttpRequestSpec request, HttpClientPlatformException e) {
        HttpEventType type = switch (e.errorCode()) {
            case REQUEST_CONSTRUCTION_FAILED -> HttpEventType.REQUEST_CONSTRUCTION_FAILED;
            case SERIALIZATION_FAILED -> HttpEventType.SERIALIZATION_FAILED;
            case CONNECTION_FAILED -> HttpEventType.CONNECTION_FAILED;
            case DNS_FAILURE -> HttpEventType.DNS_FAILURE;
            case TIMEOUT -> HttpEventType.TIMEOUT;
            case TLS_FAILURE -> HttpEventType.TLS_FAILURE;
            case DOWNSTREAM_CLIENT_ERROR -> HttpEventType.DOWNSTREAM_CLIENT_ERROR;
            case DOWNSTREAM_SERVER_ERROR -> HttpEventType.DOWNSTREAM_SERVER_ERROR;
            case DESERIALIZATION_FAILED -> HttpEventType.DESERIALIZATION_FAILED;
            case FORWARDING_FAILED -> HttpEventType.FORWARDING_FAILED;
            case MIDDLEWARE_FAILURE -> HttpEventType.MIDDLEWARE_FAILURE;
            case UNEXPECTED -> HttpEventType.UNEXPECTED_ERROR;
        };
        return HttpEvent.of(type, request, e, e.stage(), e.metadata());
    }

    private static String contentTypeOf(HttpResponse<?> response) {
        Map<String, List<String>> headers = response.headers();
        List<String> values = headers.getOrDefault("Content-Type", headers.getOrDefault("content-type", List.of()));
        return values.isEmpty() ? "application/octet-stream" : values.get(0);
    }
}
