package com.example.product.infra;

public class ProductRepository {
    private static final String BASE_URL = "https://api.example.com";
    private static final String PRODUCT_PATH = "/api/products";
    private Client client;

    public void createProduct(String productData) {
        // Create product using HTTP client
        client.post(BASE_URL + PRODUCT_PATH, productData);
    }

    public void updateProduct(String productData) {
        // Update product via HTTP
        client.post(BASE_URL + PRODUCT_PATH + "/update", productData);
    }

    public void deleteProduct() {
        String endpoint = "/delete";
        client.post(BASE_URL + PRODUCT_PATH + endpoint, "delete data");
    }

    public String getProduct(String productId) {
        // Get product via HTTP
        return client.get(BASE_URL + PRODUCT_PATH + "/" + productId);
    }

    public String listProducts() {
        return client.get(BASE_URL + PRODUCT_PATH);
    }
}
