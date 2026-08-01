package com.example;
import org.jmolecules.ddd.annotation.Repository;
@Repository
public interface CustomerRepository {
    Object findById(String id);
}
