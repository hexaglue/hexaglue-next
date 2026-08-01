package com.example.ports.in;
import org.jmolecules.architecture.hexagonal.PrimaryPort;
@PrimaryPort
public interface PlaceOrderUseCase {
    void forOrder(Object order);
}
