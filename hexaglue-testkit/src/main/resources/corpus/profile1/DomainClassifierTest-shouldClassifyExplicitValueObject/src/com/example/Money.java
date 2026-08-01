package com.example;
import org.jmolecules.ddd.annotation.ValueObject;
@ValueObject
public record Money(int amount, String currency) {}
