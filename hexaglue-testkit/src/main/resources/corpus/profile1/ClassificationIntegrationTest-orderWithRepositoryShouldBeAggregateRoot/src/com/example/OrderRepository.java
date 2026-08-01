package com.example;
public interface OrderRepository {
    Order findById(String id);
    void save(Order order);
    java.util.List<Order> findAll();
}
