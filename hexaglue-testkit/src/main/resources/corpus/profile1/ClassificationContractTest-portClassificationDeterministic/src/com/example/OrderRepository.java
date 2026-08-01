package com.example;
public interface OrderRepository {
    Object findById(String id);
    void save(Object order);
}
