package com.example.stock_management_system;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;

import java.util.Optional;

public class DeleteAllController {

    @FXML
    private Button btnConfirmDeleteAll;

    @FXML
    private void initialize() {
        btnConfirmDeleteAll.setOnAction(e -> confirmDelete());
    }

    private void confirmDelete() {

        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Confirm Delete");
        confirmAlert.setHeaderText("Delete All Data");
        confirmAlert.setContentText(
                "Are you sure you want to permanently delete ALL data?\n\nThis action cannot be undone!"
        );

        Optional<ButtonType> result = confirmAlert.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.OK) {
            deleteAllData();
        }
    }

    private void deleteAllData() {
        System.out.println("All data deleted (mock).");

        Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
        successAlert.setTitle("Success");
        successAlert.setHeaderText(null);
        successAlert.setContentText("All data has been deleted successfully.");
        successAlert.showAndWait();
    }
}
