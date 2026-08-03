package com.example.shop;

/** Where orders are kept. */
public interface Orders {
    /**
     * Stores an order.
     *
     * @param order the order to store
     */
    void save(Order order);
}
