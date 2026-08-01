package com.example;
import org.jmolecules.ddd.annotation.Repository;
@Repository
public interface CustomerStore {
    Object find(String id);
}
