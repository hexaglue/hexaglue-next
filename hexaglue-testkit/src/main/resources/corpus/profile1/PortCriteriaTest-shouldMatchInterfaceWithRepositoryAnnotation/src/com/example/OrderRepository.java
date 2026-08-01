package com.example;
import org.jmolecules.ddd.annotation.Repository;
@Repository
public interface OrderRepository {
    void save(Object order);
}
