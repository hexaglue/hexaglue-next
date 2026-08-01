package com.example;
import org.jmolecules.architecture.hexagonal.SecondaryPort;
public interface PaymentGateway extends SecondaryPort {
    void processPayment(Object payment);
}
