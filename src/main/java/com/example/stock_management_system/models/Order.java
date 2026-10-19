package com.example.stock_management_system.models;

import java.time.LocalDate;

public record Order(int id,
                    String productName,
                    int quantity,
                    double totalPrice,
                    LocalDate orderDate) {
}
