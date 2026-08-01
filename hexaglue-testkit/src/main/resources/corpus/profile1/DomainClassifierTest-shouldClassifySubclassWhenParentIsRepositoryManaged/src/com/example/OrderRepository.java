package com.example;
public interface OrderRepository {
    Order findById(java.util.UUID id);
    void save(Order order);
}
