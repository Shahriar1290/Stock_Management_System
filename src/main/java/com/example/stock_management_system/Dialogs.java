package com.example.stock_management_system;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextInputDialog;

import java.util.Optional;

/** Themed alert and prompt dialogs shared by all screens. */
public final class Dialogs {

    private Dialogs() {
    }

    public static void info(String title, String message) {
        show(Alert.AlertType.INFORMATION, title, message);
    }

    public static void warn(String title, String message) {
        show(Alert.AlertType.WARNING, title, message);
    }

    public static void error(String title, String message) {
        show(Alert.AlertType.ERROR, title, message);
    }

    public static boolean confirm(String title, String message) {
        Alert alert = create(Alert.AlertType.CONFIRMATION, title, message);
        return alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK;
    }

    public static Optional<String> prompt(String title, String message, String initialValue) {
        TextInputDialog dialog = new TextInputDialog(initialValue);
        dialog.setTitle(title);
        dialog.setHeaderText(null);
        dialog.setContentText(message);
        Navigator.applyTheme(dialog.getDialogPane());
        return dialog.showAndWait().map(String::trim).filter(s -> !s.isEmpty());
    }

    private static void show(Alert.AlertType type, String title, String message) {
        create(type, title, message).showAndWait();
    }

    private static Alert create(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        Navigator.applyTheme(alert.getDialogPane());
        return alert;
    }
}
