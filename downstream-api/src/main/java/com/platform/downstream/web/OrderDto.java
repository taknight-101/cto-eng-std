package com.platform.downstream.web;

public record OrderDto(String orderId, String sku, int quantity, String status) {
}
