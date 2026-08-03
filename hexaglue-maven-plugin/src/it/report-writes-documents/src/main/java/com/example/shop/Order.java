package com.example.shop;

/** An order. */
public class Order {
    private final OrderId id;

    /**
     * Creates an order.
     *
     * @param id its identity
     */
    public Order(OrderId id) {
        this.id = id;
    }

    /**
     * Returns its identity.
     *
     * @return the identity
     */
    public OrderId id() {
        return id;
    }
}
