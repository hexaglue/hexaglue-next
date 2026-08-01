package com.example;
import org.jmolecules.ddd.annotation.Repository;
@Repository
public interface PaymentGateway {
    Object find(String id);
}
