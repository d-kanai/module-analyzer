package com.example.product.application;

import com.example.product.infra.Client;

public class ProductCommand {
    private static final String BASE_URL = "https://api.example.com";
    private static final String PRODUCT_PATH = "/api/products";
    private Client client;

    public void createProduct() {
        // Create product using HTTP client
        client.post(BASE_URL + PRODUCT_PATH, "product data");
    }

    public void updateProduct() {
        // Update product via HTTP
        client.post(BASE_URL + PRODUCT_PATH + "/update", "updated product");
    }

    public void deleteProduct() {
        String endpoint = "/delete";
        client.post(BASE_URL + PRODUCT_PATH + endpoint, "delete data");
    }
}
