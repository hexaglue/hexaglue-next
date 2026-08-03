package com.example.shop;

/** What the application does with orders: it places them, through the way out it is handed. */
public class PlaceOrder {

    private final Orders orders;

    /**
     * Creates the use case.
     *
     * @param orders where orders are kept
     */
    public PlaceOrder(Orders orders) {
        this.orders = orders;
    }

    /**
     * Places an order.
     *
     * @param id the identity to give it
     * @param total what it costs
     */
    public void place(OrderId id, Money total) {
        orders.save(new Order(id, total));
    }
}
