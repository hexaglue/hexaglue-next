package com.coffeeshop.ports.in;
import com.coffeeshop.domain.order.*;
import java.util.Optional;
import org.jmolecules.architecture.hexagonal.PrimaryPort;
@PrimaryPort
public interface OrderingCoffee {
    Order createOrder(String customerName, Location location);
    Optional<Order> findOrder(OrderId id);
}
