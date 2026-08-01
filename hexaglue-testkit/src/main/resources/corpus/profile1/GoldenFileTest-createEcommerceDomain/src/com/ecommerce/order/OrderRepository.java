package com.ecommerce.order;
import java.util.Optional;
import org.jmolecules.ddd.annotation.Repository;
@Repository
public interface OrderRepository {
    Order save(Order order);
    Optional<Order> findById(OrderId id);
}
