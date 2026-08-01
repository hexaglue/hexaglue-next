package com.example.ports.in;
public interface CustomerRepository {
    Object findById(String id);
    void save(Object entity);
}
