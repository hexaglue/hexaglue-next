package com.example;
import org.jmolecules.ddd.annotation.ValueObject;
@ValueObject
public class Money {
    private String id;  // Doesn't trigger any criteria (has-identity removed)
    private int amount;
    private String currency;
}
