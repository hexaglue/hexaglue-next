package com.example;
import com.example.domain.Order;
public interface PlaceOrderUseCase {
    Order execute(String customerId);
}
