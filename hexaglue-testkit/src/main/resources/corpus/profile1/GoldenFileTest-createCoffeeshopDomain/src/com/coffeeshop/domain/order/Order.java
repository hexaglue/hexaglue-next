package com.coffeeshop.domain.order;
import java.util.List;
public class Order {
    private final OrderId id;
    private final String customerName;
    private final Location location;
    private final List<LineItem> items;
    public Order(OrderId id, String customerName, Location location, List<LineItem> items) {
        this.id = id;
        this.customerName = customerName;
        this.location = location;
        this.items = items;
    }
    public OrderId getId() { return id; }
    public String getCustomerName() { return customerName; }
    public Location getLocation() { return location; }
    public List<LineItem> getItems() { return items; }
}
