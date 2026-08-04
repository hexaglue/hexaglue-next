package com.example.shop;

import java.util.Optional;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

/**
 * How Orders is answered from the store.
 *
 * <p>Written from the classified model. Anything changed here is lost the next time the sources are read.</p>
 */
@Generated("io.hexaglue.jpa")
@Component
public final class OrdersJpaAdapter implements Orders {
    private final OrderJpaRepository repository;

    /**
     * @param repository what holds the rows
     */
    public OrdersJpaAdapter(OrderJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<Order> findById(OrderId id) {
        return repository.findById(id.value()).map(OrderMapper::toDomain);
    }

    @Override
    public void save(Order order) {
        repository.save(OrderMapper.toEntity(order));
    }
}
