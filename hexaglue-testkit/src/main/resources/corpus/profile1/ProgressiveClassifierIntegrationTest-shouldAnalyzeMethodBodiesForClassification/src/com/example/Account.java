package com.example;
public class Account {
    private String id;
    private double balance;

    public void deposit(double amount) {
        this.balance += amount;  // Field write - mutation
    }

    public double getBalance() {
        return balance;  // Field read only
    }
}
