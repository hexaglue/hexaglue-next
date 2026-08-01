package com.example;
import org.jmolecules.ddd.annotation.Repository;
@Repository
public interface OrderRepository {
    Object findById(String id);
}
