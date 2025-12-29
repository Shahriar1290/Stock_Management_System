package com.example.stock_management_system;

import com.example.stock_management_system.models.User;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public class ProfileController {

    @FXML private TextField txtUsername;
    @FXML private TextField txtName;
    @FXML private TextField txtEmail;
    @FXML private TextField txtRole;

    @FXML private PasswordField txtPassword;
    @FXML private PasswordField txtConfirmPassword;

    @FXML private Button btnUpdateProfile;
    @FXML private Button btnCancel;

    private DatabaseManager databaseManager;

    @FXML
    public void initialize() {
        databaseManager = new DatabaseManager();
        loadUserProfile();
    }

    private void loadUserProfile() {

        String username = Session.getUser();

        if (username == null) {
            showAlert(Alert.AlertType.ERROR,
                    "Session Error",
                    "No logged-in user found.");
            return;
        }

        User user = databaseManager.getUserByUsername(username);

        if (user == null) {
            showAlert(Alert.AlertType.ERROR,
                    "Error",
                    "User not found.");
            return;
        }

        txtUsername.setText(user.getUsername());
        txtName.setText(user.getName());
        txtEmail.setText(user.getEmail());
        txtRole.setText(user.getRole());

        txtPassword.clear();
        txtConfirmPassword.clear();
    }

    @FXML
    private void updateProfile() {

        if (txtName.getText().isBlank() || txtEmail.getText().isBlank()) {
            showAlert(Alert.AlertType.ERROR,
                    "Validation Error",
                    "Name and Email are required.");
            return;
        }

        String newPassword = txtPassword.getText();
        String confirmPassword = txtConfirmPassword.getText();

        if (!newPassword.isBlank() || !confirmPassword.isBlank()) {

            if (!newPassword.equals(confirmPassword)) {
                showAlert(Alert.AlertType.ERROR,
                        "Password Error",
                        "Passwords do not match.");
                return;
            }

            databaseManager.updateUserProfile(
                    txtUsername.getText(),
                    txtName.getText(),
                    txtEmail.getText(),
                    PasswordUtil.hashPassword(newPassword)
            );

        } else {
            databaseManager.updateUserProfile(
                    txtUsername.getText(),
                    txtName.getText(),
                    txtEmail.getText(),
                    null
            );
        }

        showAlert(Alert.AlertType.INFORMATION,
                "Success",
                "Profile updated successfully.");

        txtPassword.clear();
        txtConfirmPassword.clear();
    }

    @FXML
    private void cancelUpdate() {
        loadUserProfile();
    }

    private void showAlert(Alert.AlertType type,
                           String title,
                           String message) {

        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
