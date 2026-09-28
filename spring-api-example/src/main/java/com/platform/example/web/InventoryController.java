package com.platform.example.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Simulated downstream service, living in the same process purely so this example is
 * runnable without any external dependency. In production this would be a separate service
 * reached over the network via {@code http-client-lib}; this controller lives under {@code
 * /internal/**}, which {@link com.platform.example.config.SecurityConfig} deliberately keeps
 * outside the security platform's filter chain.
 */
@RestController
@RequestMapping("/internal/inventory")
public class InventoryController {

    @GetMapping("/{sku}")
    public InventoryResponse inventory(@PathVariable String sku) {
        int quantity = sku.startsWith("OUT") ? 0 : 100;
        return new InventoryResponse(sku, quantity);
    }
}
