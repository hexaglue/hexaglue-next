package com.example;
import org.jmolecules.architecture.hexagonal.PrimaryPort;
@PrimaryPort
public interface PlaceOrderUseCase {
    void execute(Object command);
}
