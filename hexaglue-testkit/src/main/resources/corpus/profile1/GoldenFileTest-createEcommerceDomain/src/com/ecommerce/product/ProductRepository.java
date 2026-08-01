package com.ecommerce.product;
import java.util.Optional;
import java.util.List;
import org.jmolecules.ddd.annotation.Repository;
@Repository
public interface ProductRepository {
    Product save(Product product);
    Optional<Product> findById(ProductId id);
    List<Product> findAll();
}
