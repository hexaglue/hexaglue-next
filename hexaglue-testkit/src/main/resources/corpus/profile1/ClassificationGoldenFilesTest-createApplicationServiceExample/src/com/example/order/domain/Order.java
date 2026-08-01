package com.example.order.domain;
public class Order {
    private final OrderId id;
    private String product;

    public Order(OrderId id, String product) {
        this.id = id;
        this.product = product;
    }

    public OrderId getId() { return id; }
    public String getProduct() { return product; }
}
