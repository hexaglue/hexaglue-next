package com.example.ports.out;
import org.jmolecules.ddd.annotation.Repository;
@Repository
public interface OrderRepository {
    Object findById(String id);
    void save(Object order);
}
