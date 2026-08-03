package com.example.shop;

/** An order, identified by its own identity and carrying what it costs. */
public class Order {

    private final OrderId id;
    private final Money total;

    /**
     * Creates an order.
     *
     * @param id its identity
     * @param total what it costs
     */
    public Order(OrderId id, Money total) {
        this.id = id;
        this.total = total;
    }

    /**
     * Returns its identity.
     *
     * @return the identity
     */
    public OrderId id() {
        return id;
    }

    /**
     * Returns what it costs.
     *
     * @return the total
     */
    public Money total() {
        return total;
    }
}
