package com.example.stock_management_system;

import com.example.stock_management_system.models.User;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class ProfileController {

    @FXML private TextField txtName;
    @FXML private TextField txtEmail;
    @FXML private PasswordField txtPassword;
    @FXML private Button btnUpdateProfile;

    private final User currentUser =
            new User("Admin User", "admin@example.com", "1234");

    @FXML
    private void initialize() {
        txtName.setText(currentUser.getName());
        txtEmail.setText(currentUser.getEmail());
        txtPassword.setText(currentUser.getPassword());
    }

    @FXML
    private void updateProfile() {

        if (txtName.getText().isEmpty() ||
                txtEmail.getText().isEmpty() ||
                txtPassword.getText().isEmpty()) {

            showAlert(Alert.AlertType.ERROR, "Validation Error", "All fields are required.");
            return;
        }

        currentUser.setName(txtName.getText());
        currentUser.setEmail(txtEmail.getText());
        currentUser.setPassword(txtPassword.getText());

        showAlert(Alert.AlertType.INFORMATION, "Success", "Profile updated successfully!");
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public Button getBtnUpdateProfile() {
        return btnUpdateProfile;
    }

    public void setBtnUpdateProfile(Button btnUpdateProfile) {
        this.btnUpdateProfile = btnUpdateProfile;
    }
}
