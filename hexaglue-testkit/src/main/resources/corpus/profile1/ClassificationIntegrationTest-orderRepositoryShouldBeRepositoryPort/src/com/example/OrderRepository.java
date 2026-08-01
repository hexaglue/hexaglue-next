package com.example;
public interface OrderRepository {
    Order findById(String id);
    void save(Order order);
}
