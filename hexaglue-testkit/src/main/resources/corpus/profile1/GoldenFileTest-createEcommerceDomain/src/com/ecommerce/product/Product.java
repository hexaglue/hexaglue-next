package com.ecommerce.product;
import java.math.BigDecimal;
public class Product {
    private final ProductId id;
    private String name;
    private String description;
    private BigDecimal price;
    private int stockQuantity;
    public Product(ProductId id, String name, BigDecimal price) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.stockQuantity = 0;
    }
    public ProductId getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public BigDecimal getPrice() { return price; }
    public int getStockQuantity() { return stockQuantity; }
    public void addStock(int quantity) { this.stockQuantity += quantity; }
}
