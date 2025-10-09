package com.example.order.application;

import com.example.product.expose.FindProductApi;
import com.example.user.expose.FindUserApi;
import com.example.notification.expose.SendNotificationApi;
import com.example.order.infra.Client;
import com.example.order.infra.OrderRepository;

public class OrderCommand {
    private FindProductApi productApi;
    private FindUserApi userApi;
    private SendNotificationApi notificationApi;
    private Client client;
    private OrderRepository orderRepository;

    public void createOrder() {
        // Create order using HTTP client
        client.post("/api/orders", "order data");
    }

    public void placeOrder() {
        // Place order via repository
        orderRepository.save("new order data");
    }
}
