package com.coffeeshop.order;
import org.jmolecules.ddd.annotation.Repository;
@Repository
public interface OrderRepository {
    Order findById(OrderId id);
    void save(Order order);
}
