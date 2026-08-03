package org.springframework.data.jpa.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.repository.Repository;

public interface JpaRepository<T, ID> extends Repository<T, ID> {

    T save(T entity);

    Optional<T> findById(ID id);

    List<T> findAll();

    void delete(T entity);
}
