package com.platform.downstream.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Called by spring-api-example's OrderService/GatewayController via http-client-lib. */
@RestController
@RequestMapping("/internal/inventory")
public class InventoryController {

    @GetMapping("/{sku}")
    public InventoryDto inventory(@PathVariable String sku) {
        int quantity = sku.startsWith("OUT") ? 0 : 100;
        return new InventoryDto(sku, quantity);
    }
}
