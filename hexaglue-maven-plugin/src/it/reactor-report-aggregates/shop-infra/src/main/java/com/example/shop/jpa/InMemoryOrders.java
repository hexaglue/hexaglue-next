package com.example.shop.jpa;

import com.example.shop.Order;
import com.example.shop.OrderId;
import com.example.shop.Orders;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/** What answers the way out, in another module than the one that declared it. */
public class InMemoryOrders implements Orders {

    private final Map<OrderId, Order> stored = new HashMap<>();

    @Override
    public void save(Order order) {
        stored.put(order.id(), order);
    }

    @Override
    public Optional<Order> findById(OrderId id) {
        return Optional.ofNullable(stored.get(id));
    }
}
