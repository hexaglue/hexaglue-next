package com.ecommerce.order;
import java.util.List;
import java.time.Instant;
public class Order {
    private final OrderId id;
    private final String customerId;
    private final List<String> productIds;
    private final Instant createdAt;
    private String status;
    public Order(OrderId id, String customerId, List<String> productIds) {
        this.id = id;
        this.customerId = customerId;
        this.productIds = productIds;
        this.createdAt = Instant.now();
        this.status = "PENDING";
    }
    public OrderId getId() { return id; }
    public String getCustomerId() { return customerId; }
    public List<String> getProductIds() { return productIds; }
    public Instant getCreatedAt() { return createdAt; }
    public String getStatus() { return status; }
    public void confirm() { this.status = "CONFIRMED"; }
}
