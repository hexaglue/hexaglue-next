package com.ecommerce.customer;
import java.util.Optional;
import org.jmolecules.ddd.annotation.Repository;
@Repository
public interface CustomerRepository {
    Customer save(Customer customer);
    Optional<Customer> findById(CustomerId id);
}
