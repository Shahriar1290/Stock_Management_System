package com.example.stock_management_system.models;

import java.time.LocalDate;

public class Order {

    private int id;
    private String productName;
    private int quantity;
    private double totalPrice;
    private LocalDate orderDate;   // ✅ MUST be LocalDate

    public Order(int id, String productName, int quantity,
                 double totalPrice, LocalDate orderDate) {
        this.id = id;
        this.productName = productName;
        this.quantity = quantity;
        this.totalPrice = totalPrice;
        this.orderDate = orderDate;
    }

    public int getId() {
        return id;
    }

    public String getProductName() {
        return productName;
    }

    public int getQuantity() {
        return quantity;
    }

    public double getTotalPrice() {
        return totalPrice;
    }

    public LocalDate getOrderDate() {
        return orderDate;
    }
}
