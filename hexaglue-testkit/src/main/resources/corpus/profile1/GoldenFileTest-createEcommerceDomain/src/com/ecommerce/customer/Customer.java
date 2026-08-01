package com.ecommerce.customer;
public class Customer {
    private final CustomerId id;
    private String name;
    private String email;
    public Customer(CustomerId id, String name, String email) {
        this.id = id;
        this.name = name;
        this.email = email;
    }
    public CustomerId getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public void updateEmail(String newEmail) { this.email = newEmail; }
}
