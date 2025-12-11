package com.example.stock_management_system.models;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class Product {

    private final IntegerProperty id;
    private final StringProperty name;
    private final StringProperty category;
    private final IntegerProperty stock;

    public Product(int id, String name, String category, int stock) {
        this.id = new SimpleIntegerProperty(id);
        this.name = new SimpleStringProperty(name);
        this.category = new SimpleStringProperty(category);
        this.stock = new SimpleIntegerProperty(stock);
    }

    public IntegerProperty idProperty() {
        return id;
    }

    public StringProperty nameProperty() {
        return name;
    }

    public StringProperty categoryProperty() {
        return category;
    }

    public IntegerProperty stockProperty() {
        return stock;
    }

    public int getId() {
        return id.get();
    }

    public String getName() {
        return name.get();
    }

    public String getCategory() {
        return category.get();
    }

    public int getStock() {
        return stock.get();
    }

    public void setName(String name) {
        this.name.set(name);
    }

    public void setCategory(String category) {
        this.category.set(category);
    }

    public void setStock(int stock) {
        this.stock.set(stock);
    }
}
