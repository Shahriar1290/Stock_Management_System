package com.example.stock_management_system.models;

public record User(String username, String name, String email, String role) {

    public static final String ADMIN = "ADMIN";
    public static final String STAFF = "STAFF";

    public boolean isAdmin() {
        return ADMIN.equalsIgnoreCase(role);
    }
}
