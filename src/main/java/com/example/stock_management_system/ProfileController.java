package com.example.stock_management_system;

import com.example.stock_management_system.models.User;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class ProfileController {

    @FXML private TextField txtUsername;
    @FXML private TextField txtRole;
    @FXML private TextField txtName;
    @FXML private TextField txtEmail;

    @FXML private Label passwordHint;
    @FXML private PasswordField txtCurrentPassword;
    @FXML private PasswordField txtPassword;
    @FXML private PasswordField txtConfirmPassword;

    private final DatabaseManager db = DatabaseManager.getInstance();

    @FXML
    public void initialize() {
        passwordHint.setText("Enter your current password, then a new one of at least "
                + PasswordUtil.MIN_LENGTH + " characters.");
        loadUserProfile();
    }

    private void loadUserProfile() {
        User user = Session.getUser();
        if (user == null) {
            Dialogs.error("Session error", "No signed-in user found.");
            return;
        }
        txtUsername.setText(user.username());
        txtRole.setText(user.role());
        txtName.setText(user.name());
        txtEmail.setText(user.email());
    }

    @FXML
    private void updateProfile() {
        User user = Session.getUser();
        String name = txtName.getText().trim();
        String email = txtEmail.getText().trim();

        if (name.isEmpty() || email.isEmpty()) {
            Dialogs.warn("Missing information", "Name and email are required.");
            return;
        }
        if (!email.contains("@")) {
            Dialogs.warn("Invalid email", "Please enter a valid email address.");
            return;
        }
        if (!db.updateUserProfile(user.username(), name, email)) {
            Dialogs.error("Error", "Your details could not be saved.");
            return;
        }
        Session.setUser(new User(user.username(), name, email, user.role()));
        Dialogs.info("Profile updated", "Your details have been saved.");
    }

    @FXML
    private void changePassword() {
        String username = Session.getUser().username();
        String newPassword = txtPassword.getText();

        if (!db.validateUser(username, txtCurrentPassword.getText())) {
            Dialogs.warn("Incorrect password", "Your current password is incorrect.");
        } else if (newPassword.length() < PasswordUtil.MIN_LENGTH) {
            Dialogs.warn("Password too short",
                    "The new password must be at least " + PasswordUtil.MIN_LENGTH + " characters.");
        } else if (!newPassword.equals(txtConfirmPassword.getText())) {
            Dialogs.warn("Passwords do not match", "The new password and confirmation do not match.");
        } else if (!db.updatePassword(username, newPassword)) {
            Dialogs.error("Error", "Your password could not be changed.");
        } else {
            Dialogs.info("Password changed", "Your password has been updated.");
            txtCurrentPassword.clear();
            txtPassword.clear();
            txtConfirmPassword.clear();
        }
    }

    @FXML
    private void cancelUpdate() {
        loadUserProfile();
    }
}
