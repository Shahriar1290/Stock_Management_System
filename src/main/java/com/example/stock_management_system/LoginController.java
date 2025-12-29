package com.example.stock_management_system;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

public class LoginController {

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Label errorLabel;

    private DatabaseManager databaseManager;

    @FXML
    public void initialize() {
        databaseManager = new DatabaseManager();
    }

    @FXML
    private void handleLogin() {

        String username = usernameField.getText();
        String password = passwordField.getText();

        if (username == null || username.isBlank()
                || password == null || password.isBlank()) {

            errorLabel.setText("Please enter username and password");
            return;
        }

        boolean isValid = databaseManager.validateUser(username, password);

        if (isValid) {
            Session.setUser(username);

            loadDashboard();
        } else {
            errorLabel.setText("Invalid username or password");
        }
    }

    private void loadDashboard() {
        try {
            FXMLLoader loader =
                    new FXMLLoader(getClass().getResource(
                            "/com/example/stock_management_system/HomePage.fxml"
                    ));

            Scene dashboardScene = new Scene(loader.load());

            Stage stage = (Stage) usernameField.getScene().getWindow();
            stage.setTitle("Stock Management System - Dashboard");
            stage.setScene(dashboardScene);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
