package com.example.shop;

import javax.annotation.processing.Generated;

/**
 * Between Order and the row that stores it.
 *
 * <p>Written from the classified model. Anything changed here is lost the next time the sources are read.</p>
 */
@Generated("io.hexaglue.jpa")
public final class OrderMapper {
    private OrderMapper() {
    }

    /**
     * @param domain what to store
     * @return the row storing it
     */
    public static OrderEntity toEntity(Order domain) {
        return new OrderEntity(domain.id().value(), MoneyMapper.toEntity(domain.total()));
    }

    /**
     * @param row what was stored
     * @return what the domain makes of it
     */
    public static Order toDomain(OrderEntity row) {
        return new Order(new OrderId(row.getId()), MoneyMapper.toDomain(row.getTotal()));
    }
}
