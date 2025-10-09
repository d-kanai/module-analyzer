package com.example.order.infra;

public class OrderRepository {
    private static final String API_BASE = "https://orders.example.com";
    private static final String SAVE_ENDPOINT = "/api/orders/save";
    private Client client;

    public void save(String orderData) {
        // Save order via HTTP client
        client.post(API_BASE + SAVE_ENDPOINT, orderData);
    }
}
