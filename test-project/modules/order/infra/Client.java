package com.example.order.infra;

public class Client {
    public void post(String url, String data) {
        // HTTP POST implementation
        System.out.println("Posting to: " + url);
    }

    public void get(String url) {
        // HTTP GET implementation
        System.out.println("Getting from: " + url);
    }
}
