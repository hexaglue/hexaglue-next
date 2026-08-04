package com.example.shop;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import javax.annotation.processing.Generated;

/**
 * How Order is stored.
 *
 * <p>Written from the classified model. Anything changed here is lost the next time the sources are read.</p>
 */
@Generated("io.hexaglue.jpa")
@Entity
@Table(
        name = "orders"
)
public class OrderEntity {
    @Id
    @Column(
            name = "id"
    )
    private UUID id;

    @Embedded
    private MoneyEmbeddable total;

    /**
     * For the persistence provider.
     */
    protected OrderEntity() {
    }

    /**
     * Builds a row from what the domain holds.
     * @param id the stored id
     * @param total the stored total
     */
    public OrderEntity(UUID id, MoneyEmbeddable total) {
        this.id = id;
        this.total = total;
    }

    /**
     * @return the stored id
     */
    public UUID getId() {
        return id;
    }

    /**
     * @return the stored total
     */
    public MoneyEmbeddable getTotal() {
        return total;
    }
}
