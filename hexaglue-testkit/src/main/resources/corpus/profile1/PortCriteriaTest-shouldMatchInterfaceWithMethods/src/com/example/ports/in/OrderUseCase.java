package com.example.ports.in;
public interface OrderUseCase {
    void createOrder(String customerId);
    void cancelOrder(String orderId);
}
