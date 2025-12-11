package com.example.stock_management_system.models;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class Order {

    private final IntegerProperty orderId;
    private final StringProperty product;
    private final IntegerProperty quantity;
    private final StringProperty date;

    public Order(int orderId, String product, int quantity, String date) {
        this.orderId = new SimpleIntegerProperty(orderId);
        this.product = new SimpleStringProperty(product);
        this.quantity = new SimpleIntegerProperty(quantity);
        this.date = new SimpleStringProperty(date);
    }

    public IntegerProperty orderIdProperty() {
        return orderId;
    }

    public StringProperty productProperty() {
        return product;
    }

    public IntegerProperty quantityProperty() {
        return quantity;
    }

    public StringProperty dateProperty() {
        return date;
    }

    public int getOrderId() {
        return orderId.get();
    }

    public String getProduct() {
        return product.get();
    }

    public int getQuantity() {
        return quantity.get();
    }

    public String getDate() {
        return date.get();
    }

    public void setProduct(String product) {
        this.product.set(product);
    }

    public void setQuantity(int quantity) {
        this.quantity.set(quantity);
    }

    public void setDate(String date) {
        this.date.set(date);
    }
}
