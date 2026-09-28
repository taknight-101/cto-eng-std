package com.platform.example.web;

import com.platform.http.api.ForwardingException;
import com.platform.http.api.HttpClient;
import com.platform.http.api.HttpRequestSpec;
import com.platform.http.api.HttpResponse;
import com.platform.http.forwarding.ForwardingDecision;
import com.platform.http.forwarding.ForwardingExecutor;
import com.platform.http.forwarding.ForwardingPolicy;
import com.platform.security.spring.CurrentSecurityContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * {@code GET /api/gateway/inventory/{sku}}: demonstrates conditional downstream forwarding.
 * SKUs prefixed {@code BLOCKED} are rejected without ever reaching the downstream call.
 */
@RestController
@RequestMapping("/api/gateway")
public class GatewayController {

    private final ForwardingExecutor forwardingExecutor;
    private final String downstreamBaseUrl;

    public GatewayController(HttpClient httpClient, @Value("${downstream.base-url}") String downstreamBaseUrl) {
        this.downstreamBaseUrl = downstreamBaseUrl;
        ForwardingPolicy policy = (request, localResponse) -> request.uri().contains("/BLOCKED")
                ? ForwardingDecision.reject("SKU is blocked by gateway policy")
                : ForwardingDecision.forward(request);
        this.forwardingExecutor = new ForwardingExecutor(policy, req -> httpClient.execute(req, InventoryResponse.class));
    }

    @GetMapping("/inventory/{sku}")
    public ResponseEntity<?> inventory(@PathVariable String sku) {
        HttpRequestSpec.Builder builder = HttpRequestSpec.get(downstreamBaseUrl + "/internal/inventory/" + sku);
        CurrentSecurityContext.current().ifPresent(ctx -> builder.correlationId(ctx.correlationId()));
        HttpRequestSpec request = builder.build();
        try {
            HttpResponse<?> response = forwardingExecutor.execute(request, null);
            return ResponseEntity.status(response.status()).body(response.body());
        } catch (ForwardingException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        }
    }
}
