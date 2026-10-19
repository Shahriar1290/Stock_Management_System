package com.example.stock_management_system;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Objects;
import java.util.prefs.Preferences;

/** Loads FXML views, switches between the login and main screens, and applies the theme. */
public final class Navigator {

    private static final String VIEW_ROOT = "/com/example/stock_management_system/";
    private static final String STYLESHEET = Objects.requireNonNull(
            Navigator.class.getResource(VIEW_ROOT + "styles/app.css")).toExternalForm();
    private static final String DARK_CLASS = "dark";
    private static final String DARK_PREF = "darkTheme";
    private static final Preferences PREFS = Preferences.userNodeForPackage(Navigator.class);

    private static Stage stage;
    private static boolean darkTheme = PREFS.getBoolean(DARK_PREF, false);

    private Navigator() {
    }

    static void init(Stage primaryStage) {
        stage = primaryStage;
    }

    public static void showLogin() {
        Session.clear();
        setScene("LoginPage.fxml", "Stock Management System - Sign in", 1000, 640);
    }

    public static void showMain() {
        setScene("MainLayout.fxml", "Stock Management System", 1320, 820);
    }

    private static void setScene(String fxml, String title, double width, double height) {
        Parent root = load(fxml);
        applyTheme(root);
        stage.setTitle(title);
        stage.setScene(new Scene(root, width, height));
        stage.centerOnScreen();
    }

    public static Parent load(String fxml) {
        try {
            return FXMLLoader.load(Objects.requireNonNull(
                    Navigator.class.getResource(VIEW_ROOT + fxml), "Missing view " + fxml));
        } catch (IOException e) {
            throw new UncheckedIOException("Could not load " + fxml, e);
        }
    }

    public static boolean isDarkTheme() {
        return darkTheme;
    }

    public static void toggleTheme() {
        darkTheme = !darkTheme;
        PREFS.putBoolean(DARK_PREF, darkTheme);
        applyTheme(stage.getScene().getRoot());
    }

    /** Attaches the app stylesheet to a root node (scene root or dialog) and sets light/dark mode. */
    public static void applyTheme(Parent root) {
        if (!root.getStylesheets().contains(STYLESHEET)) {
            root.getStylesheets().add(STYLESHEET);
        }
        root.getStyleClass().remove(DARK_CLASS);
        if (darkTheme) {
            root.getStyleClass().add(DARK_CLASS);
        }
    }
}
