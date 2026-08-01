package com.coffeeshop.order;
import java.util.List;
import org.jmolecules.ddd.annotation.AggregateRoot;
@AggregateRoot
public class Order {
    private OrderId id;
    private List<LineItem> items;
    private Location location;
}
