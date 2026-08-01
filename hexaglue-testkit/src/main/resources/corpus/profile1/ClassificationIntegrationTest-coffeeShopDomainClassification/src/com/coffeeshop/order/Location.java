package com.coffeeshop.order;
import org.jmolecules.ddd.annotation.ValueObject;
@ValueObject
public record Location(String store, String table) {}
