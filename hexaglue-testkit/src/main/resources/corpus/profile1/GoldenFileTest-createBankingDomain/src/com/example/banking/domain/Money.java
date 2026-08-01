package com.example.banking.domain;
import java.math.BigDecimal;
import org.jmolecules.ddd.annotation.ValueObject;
@ValueObject
public record Money(BigDecimal amount, String currency) {
    public Money add(Money other) {
        if (!currency.equals(other.currency)) {
            throw new IllegalArgumentException("Cannot add different currencies");
        }
        return new Money(amount.add(other.amount), currency);
    }
}
