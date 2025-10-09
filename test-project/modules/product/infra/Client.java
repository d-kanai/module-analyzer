package com.example.product.infra;

public class Client {
    public void post(String url, String data) {
        System.out.println("Posting to: " + url);
    }

    public String get(String url) {
        System.out.println("Getting from: " + url);
        return "response";
    }
}
