package com.example.stock_management_system.models;

public record Category(int id, String name, int productCount) {

    @Override
    public String toString() {
        return name;
    }
}
