package com.coffeeshop.domain.order;
import java.math.BigDecimal;
import org.jmolecules.ddd.annotation.ValueObject;
@ValueObject
public record LineItem(String productName, int quantity, BigDecimal unitPrice) {}
