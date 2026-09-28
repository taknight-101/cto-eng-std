package com.platform.http.spring;

import com.platform.http.api.HttpConnectionException;
import com.platform.http.api.HttpErrorCode;
import com.platform.http.api.HttpMethodType;
import com.platform.http.api.HttpRequestSpec;
import com.platform.http.api.RequestBody;
import com.platform.http.api.RequestConstructionException;
import com.platform.http.spi.BodySerializer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;

import java.net.URI;
import java.net.UnknownHostException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The single point where a {@link HttpRequestSpec} is actually sent over the wire, via Spring
 * WebClient. This is the intentional adapter boundary between the library's own request/response
 * model and the underlying HTTP implementation (see root README architecture-decisions section
 * for why WebClient was chosen and how it is kept out of the public API).
 */
public final class WebClientHttpEngine implements HttpEngine {

    private final WebClient webClient;
    private final BodySerializer bodySerializer;

    public WebClientHttpEngine(WebClient webClient, BodySerializer bodySerializer) {
        this.webClient = webClient;
        this.bodySerializer = bodySerializer;
    }

    @Override
    public RawHttpResponse execute(HttpRequestSpec request) {
        Instant startedAt = Instant.now();
        byte[] requestBody = serializeBody(request);

        WebClient.RequestBodySpec spec =
                webClient.method(toSpringMethod(request.method())).uri(URI.create(request.resolvedUri()));
        request.headers().forEach((name, values) -> values.forEach(value -> spec.header(name, value)));
        if (request.acceptType() != null) {
            spec.header(HttpHeaders.ACCEPT, request.acceptType());
        }
        WebClient.RequestHeadersSpec<?> withBody = requestBody.length > 0 ? spec.bodyValue(requestBody) : spec;

        try {
            RawHttpResponse response = withBody
                    .exchangeToMono(clientResponse -> clientResponse.bodyToMono(byte[].class)
                            .defaultIfEmpty(new byte[0])
                            .map(bytes -> new RawHttpResponse(
                                    clientResponse.statusCode().value(),
                                    toMultiMap(clientResponse.headers().asHttpHeaders()),
                                    bytes,
                                    startedAt,
                                    Instant.now())))
                    .block(request.timeout());
            if (response == null) {
                throw new HttpConnectionException("No response received before timeout", null, HttpErrorCode.TIMEOUT);
            }
            return response;
        } catch (HttpConnectionException e) {
            throw e;
        } catch (Exception e) {
            throw translateFailure(e);
        }
    }

    private byte[] serializeBody(HttpRequestSpec request) {
        try {
            RequestBody body = request.body();
            if (body instanceof RequestBody.Empty) {
                return new byte[0];
            }
            if (body instanceof RequestBody.Json json) {
                return bodySerializer.serialize(json.value(), "application/json");
            }
            if (body instanceof RequestBody.Raw raw) {
                return raw.bytes();
            }
            return new byte[0];
        } catch (Exception e) {
            throw new RequestConstructionException("Failed to serialize request body", e);
        }
    }

    private static HttpMethod toSpringMethod(HttpMethodType method) {
        return HttpMethod.valueOf(method.name());
    }

    private static Map<String, List<String>> toMultiMap(HttpHeaders headers) {
        Map<String, List<String>> map = new LinkedHashMap<>();
        headers.forEach(map::put);
        return map;
    }

    private static HttpConnectionException translateFailure(Exception e) {
        Throwable rootCause = e instanceof WebClientRequestException wre ? wre.getCause() : e;
        if (rootCause instanceof UnknownHostException) {
            return new HttpConnectionException("DNS resolution failed", e, HttpErrorCode.DNS_FAILURE);
        }
        if (rootCause instanceof javax.net.ssl.SSLException) {
            return new HttpConnectionException("TLS handshake failed", e, HttpErrorCode.TLS_FAILURE);
        }
        if (e instanceof IllegalStateException && e.getMessage() != null
                && e.getMessage().toLowerCase(java.util.Locale.ROOT).contains("timeout")) {
            return new HttpConnectionException("Request timed out", e, HttpErrorCode.TIMEOUT);
        }
        return new HttpConnectionException("Connection failed", e, HttpErrorCode.CONNECTION_FAILED);
    }
}
