package com.coffeeshop.order;
import org.jmolecules.ddd.annotation.Entity;
@Entity
public class LineItem {
    private String id;
    private String productName;
    private int quantity;
}
