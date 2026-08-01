package com.example.order.ports.in;
import com.example.order.domain.Order;
import com.example.order.domain.OrderId;
import java.util.Optional;
public interface OrderUseCases {
    Order createOrder(String product);
    Optional<Order> findOrder(OrderId id);
}
