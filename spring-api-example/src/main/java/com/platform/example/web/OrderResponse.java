package com.platform.example.web;

public record OrderResponse(String orderId, String sku, int quantity, String status) {
}
