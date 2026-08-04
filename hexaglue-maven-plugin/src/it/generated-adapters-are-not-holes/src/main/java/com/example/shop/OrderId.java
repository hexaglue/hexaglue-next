package com.example.shop;

import java.util.UUID;

/**
 * The identity of an order.
 *
 * @param value the underlying value
 */
public record OrderId(UUID value) {}
