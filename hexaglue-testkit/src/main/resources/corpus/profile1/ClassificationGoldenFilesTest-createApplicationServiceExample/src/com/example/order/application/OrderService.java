package com.example.order.application;
import com.example.order.domain.Order;
import com.example.order.domain.OrderId;
import com.example.order.ports.in.OrderUseCases;
import com.example.order.ports.out.OrderRepository;
import java.util.Optional;
public class OrderService implements OrderUseCases {
    private final OrderRepository repository;

    public OrderService(OrderRepository repository) {
        this.repository = repository;
    }

    @Override
    public Order createOrder(String product) {
        Order order = new Order(OrderId.generate(), product);
        return repository.save(order);
    }

    @Override
    public Optional<Order> findOrder(OrderId id) {
        return repository.findById(id);
    }
}
