package com.example.stock_management_system;

import com.example.stock_management_system.models.User;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

/** Holds the signed-in user; observable so the UI can react to profile changes. */
public final class Session {

    private static final ObjectProperty<User> currentUser = new SimpleObjectProperty<>();

    private Session() {
    }

    public static ObjectProperty<User> userProperty() {
        return currentUser;
    }

    public static void setUser(User user) {
        currentUser.set(user);
    }

    public static User getUser() {
        return currentUser.get();
    }

    public static void clear() {
        currentUser.set(null);
    }
}
