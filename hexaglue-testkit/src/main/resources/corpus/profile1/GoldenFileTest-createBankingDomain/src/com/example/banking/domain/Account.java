package com.example.banking.domain;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
public class Account {
    private final AccountId id;
    private final String ownerName;
    private Money balance;
    private final List<Transaction> transactions;
    public Account(AccountId id, String ownerName, Money initialBalance) {
        this.id = id;
        this.ownerName = ownerName;
        this.balance = initialBalance;
        this.transactions = new ArrayList<>();
    }
    public AccountId getId() { return id; }
    public String getOwnerName() { return ownerName; }
    public Money getBalance() { return balance; }
    public List<Transaction> getTransactions() { return Collections.unmodifiableList(transactions); }
    public void deposit(Money amount) {
        this.balance = balance.add(amount);
        transactions.add(new Transaction(UUID.randomUUID(), amount, "Deposit"));
    }
}
