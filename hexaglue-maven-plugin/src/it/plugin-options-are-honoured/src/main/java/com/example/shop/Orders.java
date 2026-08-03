package com.example.shop;

import java.util.Optional;

/** The way out orders are kept behind. */
public interface Orders {

    /**
     * Stores an order.
     *
     * @param order the order to store
     */
    void save(Order order);

    /**
     * Looks an order up by its identity.
     *
     * @param id the identity to look up
     * @return the order, when there is one
     */
    Optional<Order> findById(OrderId id);
}
