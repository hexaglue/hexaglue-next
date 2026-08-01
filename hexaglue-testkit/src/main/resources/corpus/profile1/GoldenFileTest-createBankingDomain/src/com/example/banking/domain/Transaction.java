package com.example.banking.domain;
import java.time.Instant;
import java.util.UUID;
import org.jmolecules.ddd.annotation.Entity;
@Entity
public class Transaction {
    private final UUID id;
    private final Money amount;
    private final String description;
    private final Instant timestamp;
    public Transaction(UUID id, Money amount, String description) {
        this.id = id;
        this.amount = amount;
        this.description = description;
        this.timestamp = Instant.now();
    }
    public UUID getId() { return id; }
    public Money getAmount() { return amount; }
    public String getDescription() { return description; }
    public Instant getTimestamp() { return timestamp; }
}
