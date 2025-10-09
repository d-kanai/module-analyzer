package com.example.order.application;

import com.example.product.expose.FindProductApi;
import com.example.user.expose.FindUserApi;
import com.example.notification.expose.SendNotificationApi;
import com.example.order.infra.Client;
import com.example.order.infra.OrderRepository;
import com.example.order.domain.OrderService;

public class OrderCommand {
    private FindProductApi productApi;
    private FindUserApi userApi;
    private SendNotificationApi notificationApi;
    private Client client;
    private OrderRepository orderRepository;
    private OrderService orderService;

    public void createOrder() {
        // Create order using HTTP client (direct call - 0 intermediaries)
        client.post("/api/orders", "order data");
    }

    public void placeOrder() {
        // Place order via repository (1 intermediary: OrderRepository)
        orderRepository.save("new order data");
    }

    public void submitOrder() {
        // Submit order via service (2 intermediaries: OrderService -> OrderRepository)
        orderService.processOrder("submitted order");
    }

    public void testDirectCall() {
        // Test: OrderCommand -> OrderService (direct HTTP)
        orderService.directHttpCall();
    }
}
