package com.example.product.application;

import com.example.product.infra.ProductRepository;

public class ProductCommand {
    private ProductRepository productRepository;

    public void createProduct() {
        // Create product via repository
        productRepository.createProduct("product data");
    }

    public void updateProduct() {
        // Update product via repository
        productRepository.updateProduct("updated product");
    }

    public void deleteProduct() {
        // Delete product via repository
        productRepository.deleteProduct();
    }

    public void getProduct() {
        // Get product via repository
        productRepository.getProduct("123");
    }

    public void listProducts() {
        // List all products via repository
        productRepository.listProducts();
    }
}
