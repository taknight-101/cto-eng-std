package com.platform.example.web;

import com.platform.http.api.DispatchResult;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** {@code POST /api/orders}: requires {@code ROLE_ADMIN} (see {@code DemoAuthorizationPolicyConfig}). */
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<?> createOrder(@RequestBody OrderRequest request) {
        DispatchResult<OrderResponse> result = orderService.placeOrder(request);
        return switch (result) {
            case DispatchResult.Success<OrderResponse> success ->
                    ResponseEntity.status(HttpStatus.CREATED).body(success.value());
            case DispatchResult.Failure<OrderResponse> failure ->
                    ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", failure.error().getMessage()));
        };
    }
}
