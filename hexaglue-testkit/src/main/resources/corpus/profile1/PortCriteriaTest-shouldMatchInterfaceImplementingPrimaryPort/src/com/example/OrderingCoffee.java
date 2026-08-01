package com.example;
import org.jmolecules.architecture.hexagonal.PrimaryPort;
public interface OrderingCoffee extends PrimaryPort {
    void placeOrder(Object order);
}
