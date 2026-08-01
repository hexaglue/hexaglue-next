package com.example.infrastructure;
import org.jmolecules.architecture.hexagonal.SecondaryPort;
@SecondaryPort
public interface PaymentGateway {
    void charge(Object payment);
}
