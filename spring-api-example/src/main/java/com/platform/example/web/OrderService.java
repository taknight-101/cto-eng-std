package com.platform.example.web;

import com.platform.http.api.DispatchResult;
import com.platform.http.api.HttpClient;
import com.platform.http.api.HttpRequestSpec;
import com.platform.http.api.HttpResponse;
import com.platform.security.spring.CurrentSecurityContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Demonstrates: declarative HTTP client request construction, dispatch to a downstream
 * service, typed response consumption via {@link DispatchResult}, and pipeline tracing.
 */
@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final HttpClient httpClient;
    private final String downstreamBaseUrl;

    public OrderService(HttpClient httpClient, @Value("${downstream.base-url}") String downstreamBaseUrl) {
        this.httpClient = httpClient;
        this.downstreamBaseUrl = downstreamBaseUrl;
    }

    public DispatchResult<OrderResponse> placeOrder(OrderRequest request) {
        HttpRequestSpec.Builder builder = HttpRequestSpec.get(downstreamBaseUrl + "/internal/inventory/" + request.sku());
        CurrentSecurityContext.current().ifPresent(ctx -> builder.correlationId(ctx.correlationId()));
        HttpRequestSpec inventoryRequest = builder.build();

        HttpResponse<InventoryResponse> inventoryResponse = httpClient.execute(inventoryRequest, InventoryResponse.class);
        log.info("Inventory check trace for {}: {}", request.sku(), inventoryResponse.trace());

        if (!inventoryResponse.isSuccess()) {
            return DispatchResult.failure(new IllegalStateException(
                    "Inventory service returned status " + inventoryResponse.status()));
        }
        if (inventoryResponse.body().quantity() < request.quantity()) {
            return DispatchResult.failure(new IllegalStateException(
                    "Insufficient inventory for " + request.sku() + ": requested " + request.quantity()
                            + ", available " + inventoryResponse.body().quantity()));
        }
        return DispatchResult.success(
                new OrderResponse(UUID.randomUUID().toString(), request.sku(), request.quantity(), "CONFIRMED"));
    }
}
