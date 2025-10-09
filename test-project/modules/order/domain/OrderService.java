package com.example.order.domain;

import com.example.order.infra.OrderRepository;
import com.example.order.infra.Client;

public class OrderService {
    private OrderRepository orderRepository;
    private Client client;

    public void processOrder(String orderData) {
        // Business logic here
        orderRepository.save(orderData);
    }

    public void validateAndSave(String orderData) {
        // Validation logic
        orderRepository.save("validated: " + orderData);
    }

    public void directHttpCall() {
        // Direct HTTP call for testing
        client.post("https://test.com/direct", "data");
    }
}
