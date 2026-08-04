package com.example.shop;

import java.util.UUID;
import javax.annotation.processing.Generated;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * How Order is stored and found, serving Orders.
 *
 * <p>Written from the classified model. Anything changed here is lost the next time the sources are read.</p>
 */
@Generated("io.hexaglue.jpa")
public interface OrderJpaRepository extends JpaRepository<OrderEntity, UUID> {
}
