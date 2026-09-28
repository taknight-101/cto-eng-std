package com.platform.downstream.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** In-memory order store - intentionally simple; the point of this service is to be a real,
 * separately-deployed HTTP endpoint for security-platform-lib/http-client-lib to prove
 * themselves against, not to demonstrate persistence patterns. */
@RestController
@RequestMapping("/internal/orders")
public class OrdersController {

    private final Map<String, OrderDto> orders = new ConcurrentHashMap<>();

    @PostMapping
    public ResponseEntity<OrderDto> createOrder(@RequestBody OrderRequestDto request) {
        OrderDto order = new OrderDto(UUID.randomUUID().toString(), request.sku(), request.quantity(), "CREATED");
        orders.put(order.orderId(), order);
        return ResponseEntity.status(HttpStatus.CREATED).body(order);
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderDto> getOrder(@PathVariable String id) {
        OrderDto order = orders.get(id);
        return order == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(order);
    }
}
