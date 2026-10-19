package com.example.stock_management_system.models;

public record Supplier(int id, String name, String contact, int productCount) {

    @Override
    public String toString() {
        return name;
    }
}
