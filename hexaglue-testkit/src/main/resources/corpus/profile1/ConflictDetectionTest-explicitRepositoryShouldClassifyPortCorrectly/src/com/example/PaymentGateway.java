package com.example;
import org.jmolecules.ddd.annotation.Repository;
@Repository
public interface PaymentGateway {
    Object findById(String id);
}
