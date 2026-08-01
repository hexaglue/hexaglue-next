package com.example;
public interface OrderRepository {
    Order findByName(String name);
}
