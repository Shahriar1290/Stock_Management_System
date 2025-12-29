package com.example.stock_management_system;

public class Session {

    private static String loggedInUser;

    private Session() {
    }

    public static void setUser(String username) {
        loggedInUser = username;
    }

    public static String getUser() {
        return loggedInUser;
    }

    public static void clear() {
        loggedInUser = null;
    }
}
