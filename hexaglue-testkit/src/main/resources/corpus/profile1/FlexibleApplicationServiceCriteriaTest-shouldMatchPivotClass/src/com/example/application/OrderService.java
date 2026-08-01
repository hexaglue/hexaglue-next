package com.example.application;
import com.example.ports.in.OrderUseCases;
import com.example.ports.out.OrderRepository;
public class OrderService implements OrderUseCases {
    private final OrderRepository repository;
    public OrderService(OrderRepository repository) {
        this.repository = repository;
    }
    public void createOrder(String product) {
        repository.save(product);
    }
}
