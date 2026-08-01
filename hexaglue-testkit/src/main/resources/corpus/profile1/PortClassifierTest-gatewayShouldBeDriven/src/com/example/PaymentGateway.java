package com.example;
import org.jmolecules.architecture.hexagonal.SecondaryPort;
@SecondaryPort
public interface PaymentGateway {
    void process();
}
