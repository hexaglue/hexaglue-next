package com.example.shop;

import java.math.BigDecimal;

/**
 * An amount of money.
 *
 * @param amount how much
 * @param currency in what
 */
public record Money(BigDecimal amount, String currency) {}
