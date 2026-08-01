package com.example.order.ports.out;
import com.example.order.domain.Order;
import com.example.order.domain.OrderId;
import java.util.Optional;
public interface OrderRepository {
    Order save(Order order);
    Optional<Order> findById(OrderId id);
}
